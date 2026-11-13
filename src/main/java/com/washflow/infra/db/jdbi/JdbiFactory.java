package com.washflow.infra.db.jdbi;

import com.washflow.application.AppConfig;
import com.washflow.infra.db.tenancy.TenantBindingCustomizer;
import com.washflow.infra.observability.Metrics;
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
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory responsible for creating the Jdbi instance with connection pooling and schema migration.
 *
 * <p>PostgreSQL only, in every environment: {@code DATABASE_URL} (SSL, as injected by Render and
 * Railway) or the individual {@code DB_*} properties. An unreachable database stops the startup
 * instead of serving requests without data.
 */
public final class JdbiFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbiFactory.class);

  private static final String LOCAL_DEFAULT = "washflow";
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
    migrateOrClose(dataSource);
    Jdbi jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
    // Every ":tenant" gets the request's establishment (multi-lava-jato isolation).
    jdbi.getConfig(SqlStatements.class).addCustomizer(new TenantBindingCustomizer());
    return jdbi;
  }

  private static void migrateOrClose(DataSource dataSource) {
    try {
      migrate(dataSource);
    } catch (RuntimeException e) {
      if (dataSource instanceof HikariDataSource hikari) {
        hikari.close();
      }
      throw new IllegalStateException(
          "Database migration failed; refusing to start with an incomplete schema", e);
    }
  }

  /**
   * The migrations in {@code db/migration}. The history lives in {@code schema_history}; a database
   * still carrying the pre-1.5.0 schema (no such table) is baselined at version 0, so V1 removes
   * the legacy schema and V2 creates the current one.
   */
  public static void migrate(DataSource dataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .table("schema_history")
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .load()
        .migrate();
  }

  /** Creates the PostgreSQL DataSource; fails when the database can't be reached. */
  public static DataSource createDataSource(AppConfig appConfig) {
    HikariDataSource postgres = createPostgresDataSource(appConfig);
    if (isReachableWithRetry(postgres)) {
      return postgres;
    }
    String jdbcUrl = postgres.getJdbcUrl();
    postgres.close();
    throw new IllegalStateException(
        "PostgreSQL database is unreachable at "
            + jdbcUrl
            + " - start it (make db-up) or set DATABASE_URL / DB_HOST, DB_PORT, DB_NAME, DB_USER"
            + " and DB_PASSWORD");
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
    String databaseUrl = appConfig.database().url();

    if (databaseUrl == null || databaseUrl.isBlank()) {
      return createPostgresDataSourceFromProperties(appConfig);
    }

    return createPostgresDataSourceFromUrl(databaseUrl);
  }

  private static HikariDataSource createPostgresDataSourceFromProperties(AppConfig appConfig) {
    String jdbcUrl = buildFallbackJdbcUrl(appConfig);
    HikariConfig config =
        createHikariConfig(jdbcUrl, appConfig.database().user(), appConfig.database().password());
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
        + appConfig.database().host()
        + ":"
        + appConfig.database().port()
        + "/"
        + appConfig.database().name();
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
    HikariConfig metered = createUnmeteredHikariConfig(jdbcUrl, user, password);
    // Pool size, wait time and timeouts as hikaricp_* series on /api/metrics.
    metered.setMetricRegistry(Metrics.registry());
    return metered;
  }

  private static HikariConfig createUnmeteredHikariConfig(
      String jdbcUrl, String user, String password) {
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
