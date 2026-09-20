package com.washflow.infra.db.jdbi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
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
 * <p>If Postgres can't be reached at startup, falls back to an in-memory H2 database instead of
 * failing - useful for local dev/demo without {@code make db-up}. The fallback is decided once at
 * startup; it doesn't watch for Postgres coming back or going down mid-run. H2 runs in {@code
 * MODE=PostgreSQL} against {@code db/migration-h2}, a copy of the Postgres migrations with the
 * handful of Postgres-only bits (e.g. {@code gen_random_uuid()}, {@code TIMESTAMPTZ}) swapped for
 * H2 equivalents - kept in sync by hand since Flyway can't share one script across dialects that
 * differ this much.
 */
public final class JdbiFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbiFactory.class);

  // Matches docker-compose.yml's `postgres` service - the local dev default
  // when no DB_* / DATABASE_URL env vars override it.
  private static final String LOCAL_DEFAULT = "washflow";

  private static final String H2_JDBC_URL =
      "jdbc:h2:mem:washflow;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";

  private JdbiFactory() {}

  public static Jdbi create() {
    DataSource dataSource = createDataSource();
    migrate(dataSource);
    return Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
  }

  private static void migrate(DataSource dataSource) {
    String location = isH2(dataSource) ? "classpath:db/migration-h2" : "classpath:db/migration";
    try {
      Flyway.configure().dataSource(dataSource).locations(location).load().migrate();
    } catch (RuntimeException e) {
      // Same "don't block startup" reasoning as initializationFailTimeout
      // below - the database might just not be up yet (e.g. before `make
      // db-up`). Every query will fail until it is; that's the DB health
      // check's job to surface, not startup's.
      LOGGER.warn("Skipping Flyway migration - database not reachable yet: {}", e.getMessage());
    }
  }

  private static boolean isH2(DataSource dataSource) {
    return dataSource instanceof HikariDataSource hikari
        && hikari.getJdbcUrl().startsWith("jdbc:h2:");
  }

  public static DataSource createDataSource() {
    HikariDataSource postgres = createPostgresDataSource();
    if (isReachable(postgres)) {
      return postgres;
    }
    LOGGER.warn("Postgres unreachable at {} - falling back to in-memory H2", postgres.getJdbcUrl());
    postgres.close();
    return createH2DataSource();
  }

  private static boolean isReachable(HikariDataSource dataSource) {
    try (Connection ignored = dataSource.getConnection()) {
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  private static HikariDataSource createPostgresDataSource() {
    var env = System.getenv();
    String jdbcUrl;
    String user;
    String password;

    String databaseUrl = env.get("DATABASE_URL");
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      URI uri = URI.create(databaseUrl);
      String[] credentials =
          uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
      user = credentials.length > 0 ? credentials[0] : LOCAL_DEFAULT;
      password = credentials.length > 1 ? credentials[1] : "";
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
    } else {
      String host = env.getOrDefault("DB_HOST", "localhost");
      String port = env.getOrDefault("DB_PORT", "5432");
      String name = env.getOrDefault("DB_NAME", LOCAL_DEFAULT);
      jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + name;
      user = env.getOrDefault("DB_USER", LOCAL_DEFAULT);
      password = env.getOrDefault("DB_PASSWORD", LOCAL_DEFAULT);
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(user);
    config.setPassword(password);
    config.setPoolName("washflow-pool");
    config.setMaximumPoolSize(10);
    // Keep it short: the reachability probe below (and a down-Postgres
    // health check) should fail fast, not hang for Hikari's 30s default.
    config.setConnectionTimeout(3000);
    // Don't block on pool construction itself - createDataSource() decides
    // reachability with an explicit getConnection() probe right after this
    // returns, so a slow/absent Postgres surfaces as a quick H2 fallback
    // instead of a hung startup.
    config.setInitializationFailTimeout(-1);

    return new HikariDataSource(config);
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
