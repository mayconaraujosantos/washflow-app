package com.washflow.infra.db.jdbi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

/**
 * Builds the app's single {@link Jdbi} instance - the same job {@code spring-boot-starter-jdbc}
 * does via {@code application.properties}: read connection settings from the environment, pool them
 * with Hikari, done.
 *
 * <p>Reads {@code DATABASE_URL} in the {@code postgres://user:pass@host:port/db} form platforms
 * like Railway inject; otherwise falls back to discrete {@code DB_HOST}/{@code DB_PORT}/{@code
 * DB_NAME}/{@code DB_USER}/{@code DB_PASSWORD} vars, defaulting to {@code docker-compose.yml}'s
 * {@code postgres} service for local dev.
 */
public final class JdbiFactory {

  // Matches docker-compose.yml's `postgres` service - the local dev default
  // when no DB_* / DATABASE_URL env vars override it.
  private static final String LOCAL_DEFAULT = "washflow";

  private JdbiFactory() {}

  public static Jdbi create() {
    return Jdbi.create(createDataSource()).installPlugin(new SqlObjectPlugin());
  }

  public static DataSource createDataSource() {
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
    // Keep it short: a health check should fail fast, not hang for Hikari's
    // 30s default while Postgres is down.
    config.setConnectionTimeout(3000);
    // Don't block app startup if Postgres isn't reachable yet (e.g. local
    // `gradle run` before `make db-up`) - Hikari keeps retrying in the
    // background and only throws once a connection is actually requested.
    config.setInitializationFailTimeout(-1);

    return new HikariDataSource(config);
  }
}
