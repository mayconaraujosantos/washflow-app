package com.washflow.infra.db.jdbi;

import com.washflow.application.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the app's single {@link Jdbi} instance - the same job {@code spring-boot-starter-jdbc}
 * does via {@code application.properties}: read connection settings from the environment, pool them
 * with Hikari, done.
 *
 * <p>Reads {@code DATABASE_URL} in the {@code postgres://user:pass@host:port/db} form platforms
 * like Railway inject; otherwise falls back to discrete {@code DB_HOST}/{@code DB_PORT}/{@code
 * DB_NAME}/{@code DB_USER}/{@code DB_PASSWORD} vars, defaulting to {@code docker-compose.yml}'s
 * {@code postgres} service for local dev.
 *
 * <p>If Postgres can't be reached at startup, development falls back to an in-memory H2 database;
 * production fails startup. The development fallback is decided once at startup and does not
 * recover if Postgres becomes available later. H2 runs in {@code MODE=PostgreSQL} against {@code
 * db/migration-h2}, a copy of the Postgres migrations with dialect-specific differences kept in
 * sync by hand.
 */
public final class JdbiFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbiFactory.class);

  // Matches docker-compose.yml's `postgres` service - the local dev default
  // when no DB_* / DATABASE_URL env vars override it.
  private static final String LOCAL_DEFAULT = "washflow";

  private static final String H2_JDBC_URL =
      "jdbc:h2:mem:washflow;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";

  private JdbiFactory() {}

  public static Jdbi create(AppConfig appConfig) {
    DataSource dataSource = createDataSource(appConfig);
    migrate(dataSource);
    return Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
  }

  private static void migrate(DataSource dataSource) {
    String location = isH2(dataSource) ? "classpath:db/migration-h2" : "classpath:db/migration";
    try {
      Flyway.configure().dataSource(dataSource).locations(location).load().migrate();
    } catch (RuntimeException e) {
      if (dataSource instanceof HikariDataSource hikari) {
        hikari.close();
      }
      throw new IllegalStateException(
          "Database migration failed; refusing to start with an incomplete schema", e);
    }
  }

  private static boolean isH2(DataSource dataSource) {
    return dataSource instanceof HikariDataSource hikari
        && hikari.getJdbcUrl().startsWith("jdbc:h2:");
  }

  public static DataSource createDataSource(AppConfig appConfig) {
    HikariDataSource postgres = createPostgresDataSource(appConfig);
    if (isReachableWithRetry(postgres)) {
      return postgres;
    }
    if (appConfig.isProduction()) {
      postgres.close();
      throw new IllegalStateException(
          "Configured PostgreSQL database is unreachable in production");
    }
    LOGGER.warn("Postgres unreachable at {} - falling back to in-memory H2", postgres.getJdbcUrl());
    postgres.close();
    return createH2DataSource();
  }

  private static boolean isReachableWithRetry(HikariDataSource dataSource) {
    int maxAttempts = 5;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try (Connection ignored = dataSource.getConnection()) {
        return true;
      } catch (SQLException e) {
        LOGGER.warn(
            "Database connection attempt {}/{} failed: {} (jdbcUrl: {})",
            attempt,
            maxAttempts,
            e.getMessage(),
            dataSource.getJdbcUrl());
        if (attempt < maxAttempts) {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
          }
        }
      }
    }
    return false;
  }

  private static HikariDataSource createPostgresDataSource(AppConfig appConfig) {
    String jdbcUrl;

    String databaseUrl = appConfig.databaseUrl();
    String user = appConfig.databaseUser();
    String password = appConfig.databasePassword();
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      URI uri = URI.create(databaseUrl);
      String userInfo = uri.getUserInfo();
      if (userInfo != null) {
        int firstColon = userInfo.indexOf(':');
        if (firstColon > 0) {
          user = URLDecoder.decode(userInfo.substring(0, firstColon), StandardCharsets.UTF_8);
          password = URLDecoder.decode(userInfo.substring(firstColon + 1), StandardCharsets.UTF_8);
        } else {
          user = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
        }
      }
      if (user == null || user.isEmpty()) {
        user = LOCAL_DEFAULT;
      }
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String host = uri.getHost();
      // Handle IPv6 hosts that URI returns without brackets
      if (host != null && host.contains(":")) {
        host = "[" + host + "]";
      }
      jdbcUrl = "jdbc:postgresql://" + host + ":" + port + uri.getPath();
      LOGGER.info(
          "Parsed DATABASE_URL: host={}, port={}, user={}, db={}", host, port, user, uri.getPath());
    } else {
      jdbcUrl =
          "jdbc:postgresql://"
              + appConfig.databaseHost()
              + ":"
              + appConfig.databasePort()
              + "/"
              + appConfig.databaseName();
    }

    HikariConfig config = createHikariConfig(jdbcUrl, user, password);

    return new HikariDataSource(config);
  }

  @NotNull
  private static HikariConfig createHikariConfig(String jdbcUrl, String user, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(user);
    config.setPassword(password);
    config.setPoolName("washflow-pool");
    config.setMaximumPoolSize(10);
    // Increase timeout for cloud platforms (Render, Railway) where first
    // connection may take longer due to cold starts or network latency.
    config.setConnectionTimeout(10000);
    // Don't block on pool construction itself - createDataSource() decides
    // reachability with an explicit getConnection() probe right after this
    // returns, so a slow/absent Postgres surfaces as a quick H2 fallback
    // instead of a hung startup.
    config.setInitializationFailTimeout(-1);
    return config;
  }

  private static HikariDataSource createH2DataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(H2_JDBC_URL);
    config.setUsername("sa");
    config.setPassword("");
    config.setPoolName("washflow-h2-fallback-pool");
    // Keep at least one connection open for the lifetime of the pool - an
    // in-memory H2 database is dropped once its last connection closes, and
    // MODE=PostgreSQL's DB_CLOSE_DELAY=-1 only protects a single JVM-wide
    // named instance, not this pool's own connection churn.
    config.setMinimumIdle(1);
    return new HikariDataSource(config);
  }
}
