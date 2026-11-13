package com.washflow.application.config;

/** DATABASE_URL (cloud platforms) or the DB_* parts; development falls back to a local database. */
public record DatabaseConfig(
    String url, String host, int port, String name, String user, String password) {

  private static final String WASHFLOW = "washflow";

  public static DatabaseConfig from(EnvSource env, boolean production) {
    // Checked on the raw variables: the development defaults below must never reach production.
    if (production
        && env.isBlank("DATABASE_URL")
        && (env.isBlank("DB_HOST") || env.isBlank("DB_USER") || env.isBlank("DB_PASSWORD"))) {
      throw new ConfigError(
          "Production requires DATABASE_URL or DB_HOST, DB_USER, and DB_PASSWORD environment variables");
    }
    return new DatabaseConfig(
        env.getOrDefault("DATABASE_URL", ""),
        env.getOrDefault("DB_HOST", "localhost"),
        Ports.parse(env.getOrDefault("DB_PORT", "5432"), "DB_PORT"),
        env.getOrDefault("DB_NAME", WASHFLOW),
        env.getOrDefault("DB_USER", WASHFLOW),
        env.getOrDefault("DB_PASSWORD", WASHFLOW));
  }

  @Override
  public String toString() {
    return "Database[url="
        + (url.isBlank() ? "" : "[configured]")
        + ", host="
        + host
        + ", port="
        + port
        + ", name="
        + name
        + ", user="
        + user
        + ", password=[redacted]]";
  }
}
