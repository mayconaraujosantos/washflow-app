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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory responsible for creating the Jdbi instance with connection pooling and schema migration.
 *
 * <p>Supports two modes:
 *
 * <ul>
 *   <li><b>Production</b>: Uses PostgreSQL via {@code DATABASE_URL} or individual config
 *       properties. Requires SSL mode for cloud platforms like Render.
 *   <li><b>Development</b>: Falls back to in-memory H2 when PostgreSQL is unreachable.
 * </ul>
 */
public final class JdbiFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbiFactory.class);

  private static final String LOCAL_DEFAULT = "washflow";
  private static final String H2_JDBC_URL =
      "jdbc:h2:mem:washflow;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
  private static final int POSTGRES_DEFAULT_PORT = 5432;
  private static final int CONNECTION_TIMEOUT_MS = 10_000;
  private static final int MAX_POOL_SIZE = 10;
  private static final int MAX_RETRIES = 5;
  private static final long RETRY_INTERVAL_MS = 2_000L;

  private JdbiFactory() {}

  /**
   * Creates a fully configured Jdbi instance with connection pooling and Flyway migrations applied.
   */
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

  /**
   * Creates a DataSource, preferring PostgreSQL and falling back to H2 in non-production
   * environments when the database is unreachable.
   */
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

  private static HikariDataSource createH2DataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(H2_JDBC_URL);
    config.setPoolName("washflow-h2-pool");
    config.setMaximumPoolSize(5);
    // Keep at least one connection alive so the in-memory database is not dropped
    config.setMinimumIdle(1);
    // Don't block on pool construction — createDataSource() probes reachability explicitly
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  private static boolean isReachableWithRetry(HikariDataSource dataSource) {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try (Connection ignored = dataSource.getConnection()) {
        return true;
      } catch (SQLException e) {
        LOGGER.warn(
            "Database connection attempt {}/{} failed: {} (jdbcUrl: {})",
            attempt,
            MAX_RETRIES,
            e.getMessage(),
            dataSource.getJdbcUrl());
        if (attempt < MAX_RETRIES && !sleepQuietly(RETRY_INTERVAL_MS)) {
          return false;
        }
      }
    }
    return false;
  }

  private static boolean sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
      return true;
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** Immutable DTO holding parsed username and password from a DATABASE_URL. */
  private record DatabaseCredentials(String user, String password) {}

  /**
   * Creates a PostgreSQL DataSource from the application configuration.
   *
   * <p>When {@code DATABASE_URL} is present, it is parsed and SSL mode is enabled for cloud
   * platforms. Otherwise, individual properties ({@code DB_HOST}, {@code DB_USER}, etc.) are used.
   */
  private static HikariDataSource createPostgresDataSource(AppConfig appConfig) {
    String databaseUrl = appConfig.databaseUrl();

    if (databaseUrl == null || databaseUrl.isBlank()) {
      return createPostgresDataSourceFromProperties(appConfig);
    }

    return createPostgresDataSourceFromUrl(databaseUrl);
  }

  private static HikariDataSource createPostgresDataSourceFromProperties(AppConfig appConfig) {
    String jdbcUrl = buildFallbackJdbcUrl(appConfig);
    HikariConfig config =
        createHikariConfig(jdbcUrl, appConfig.databaseUser(), appConfig.databasePassword());
    return new HikariDataSource(config);
  }

  private static HikariDataSource createPostgresDataSourceFromUrl(String databaseUrl) {
    URI uri = URI.create(databaseUrl);
    DatabaseCredentials credentials = parseCredentials(uri);
    String jdbcUrl = buildJdbcUrlFromUri(uri);

    LOGGER.info(
        "Parsed DATABASE_URL: host={}, port={}, user={}, db={}",
        uri.getHost(),
        uri.getPort() > 0 ? uri.getPort() : POSTGRES_DEFAULT_PORT,
        credentials.user(),
        uri.getPath());

    HikariConfig config = createHikariConfig(jdbcUrl, credentials.user(), credentials.password());
    return new HikariDataSource(config);
  }

  private static String buildFallbackJdbcUrl(AppConfig appConfig) {
    return "jdbc:postgresql://"
        + appConfig.databaseHost()
        + ":"
        + appConfig.databasePort()
        + "/"
        + appConfig.databaseName();
  }

  /** Extracts username and password from the URI's userinfo component, URL-decoding each part. */
  private static DatabaseCredentials parseCredentials(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo == null) {
      return new DatabaseCredentials(LOCAL_DEFAULT, null);
    }

    int firstColon = userInfo.indexOf(':');
    if (firstColon <= 0) {
      String user = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
      return new DatabaseCredentials(user.isEmpty() ? LOCAL_DEFAULT : user, null);
    }

    String rawUser = userInfo.substring(0, firstColon);
    String rawPassword = userInfo.substring(firstColon + 1);

    String user = URLDecoder.decode(rawUser, StandardCharsets.UTF_8);
    String password = URLDecoder.decode(rawPassword, StandardCharsets.UTF_8);

    return new DatabaseCredentials(user.isEmpty() ? LOCAL_DEFAULT : user, password);
  }

  /**
   * Builds a JDBC URL from a DATABASE_URL URI, adding SSL mode required by cloud platforms (Render,
   * Railway, etc.) and supporting IPv6 hosts.
   */
  private static String buildJdbcUrlFromUri(URI uri) {
    String host = uri.getHost();
    if (host != null && host.contains(":")) {
      host = "[" + host + "]";
    }
    int port = uri.getPort() > 0 ? uri.getPort() : POSTGRES_DEFAULT_PORT;
    String base = "jdbc:postgresql://" + host + ":" + port + uri.getPath();
    // Cloud platforms like Render require SSL for external connections
    return base + "?sslmode=require";
  }

  private static HikariConfig createHikariConfig(String jdbcUrl, String user, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(user);
    config.setPassword(password);
    config.setPoolName("washflow-pool");
    config.setMaximumPoolSize(MAX_POOL_SIZE);
    config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
    // Don't block on pool construction — createDataSource() probes reachability explicitly
    config.setInitializationFailTimeout(-1);
    return config;
  }
}
