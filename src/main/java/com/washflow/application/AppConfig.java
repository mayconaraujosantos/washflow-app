package com.washflow.application;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/** Immutable application configuration, resolved once at startup. */
public record AppConfig(
    int port,
    String environment,
    String databaseUrl,
    String databaseHost,
    int databasePort,
    String databaseName,
    String databaseUser,
    String databasePassword,
    int maxVehiclesPerHour,
    String tokenSecret,
    String bootstrapManagerPhone,
    String bootstrapManagerPassword) {

  private static final int DEFAULT_PORT = 7000;
  private static final int DEFAULT_MAX_VEHICLES_PER_HOUR = 3;

  public static AppConfig load() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    Map<String, String> dotenvValues =
        dotenv.entries().stream()
            .collect(java.util.stream.Collectors.toMap(DotenvEntry::getKey, DotenvEntry::getValue));
    return load(System.getenv(), dotenvValues);
  }

  public static AppConfig load(Map<String, String> processEnv, Map<String, String> dotenvValues) {
    String environment =
        firstNonBlank(processEnv.get("APP_ENV"), dotenvValues.get("APP_ENV"), "development");
    boolean useDotenv = !isProduction(environment);
    String tokenSecret = value(processEnv, dotenvValues, useDotenv, "AUTH_TOKEN_SECRET", "");
    if (tokenSecret.isBlank() && !isProduction(environment)) {
      byte[] secret = new byte[32];
      new SecureRandom().nextBytes(secret);
      tokenSecret = Base64.getEncoder().encodeToString(secret);
    }

    AppConfig config =
        new AppConfig(
            parsePort(
                value(processEnv, dotenvValues, useDotenv, "PORT", String.valueOf(DEFAULT_PORT)),
                "PORT"),
            environment,
            value(processEnv, dotenvValues, useDotenv, "DATABASE_URL", ""),
            value(processEnv, dotenvValues, useDotenv, "DB_HOST", "localhost"),
            parsePort(value(processEnv, dotenvValues, useDotenv, "DB_PORT", "5432"), "DB_PORT"),
            value(processEnv, dotenvValues, useDotenv, "DB_NAME", "washflow"),
            value(processEnv, dotenvValues, useDotenv, "DB_USER", "washflow"),
            value(processEnv, dotenvValues, useDotenv, "DB_PASSWORD", "washflow"),
            parsePositiveInt(
                value(
                    processEnv,
                    dotenvValues,
                    useDotenv,
                    "MAX_VEHICLES_PER_HOUR",
                    String.valueOf(DEFAULT_MAX_VEHICLES_PER_HOUR)),
                "MAX_VEHICLES_PER_HOUR"),
            tokenSecret,
            value(processEnv, dotenvValues, useDotenv, "BOOTSTRAP_MANAGER_PHONE", "11999993333"),
            value(processEnv, dotenvValues, useDotenv, "BOOTSTRAP_MANAGER_PASSWORD", ""));

    if (isProduction(environment)
        && config.databaseUrl().isBlank()
        && (blank(processEnv.get("DB_HOST"))
            || blank(processEnv.get("DB_USER"))
            || blank(processEnv.get("DB_PASSWORD")))) {
      throw new IllegalStateException(
          "Production requires DATABASE_URL or DB_HOST, DB_USER, and DB_PASSWORD environment variables");
    }
    if (isProduction(environment) && tokenSecret.length() < 32) {
      throw new IllegalStateException(
          "Production requires AUTH_TOKEN_SECRET with at least 32 characters");
    }
    return config;
  }

  public boolean isProduction() {
    return isProduction(environment);
  }

  @Override
  public String toString() {
    return "AppConfig[port="
        + port
        + ", environment="
        + environment
        + ", databaseUrl="
        + (databaseUrl.isBlank() ? "" : "[configured]")
        + ", databaseHost="
        + databaseHost
        + ", databasePort="
        + databasePort
        + ", databaseName="
        + databaseName
        + ", databaseUser="
        + databaseUser
        + ", databasePassword=[redacted]"
        + ", maxVehiclesPerHour="
        + maxVehiclesPerHour
        + ", tokenSecret=[redacted]"
        + ", bootstrapManagerPassword=[redacted]"
        + "]";
  }

  private static boolean isProduction(String environment) {
    return "production".equalsIgnoreCase(environment);
  }

  public boolean canClaimUnconfiguredUsers() {
    return !isProduction();
  }

  private static String value(
      Map<String, String> processEnv,
      Map<String, String> dotenvValues,
      boolean useDotenv,
      String key,
      String defaultValue) {
    String processValue = processEnv.get(key);
    if (!blank(processValue)) {
      return processValue;
    }
    if (useDotenv) {
      String dotenvValue = dotenvValues.get(key);
      if (!blank(dotenvValue)) {
        return dotenvValue;
      }
    }
    return defaultValue;
  }

  private static String firstNonBlank(String first, String second, String fallback) {
    if (!blank(first)) {
      return first;
    }
    if (!blank(second)) {
      return second;
    }
    return fallback;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static int parsePositiveInt(String value, String name) {
    try {
      int parsed = Integer.parseInt(value);
      if (parsed > 0) {
        return parsed;
      }
    } catch (NumberFormatException ignored) {
      // Report a consistent configuration error below.
    }
    throw new IllegalArgumentException(name + " must be a positive integer");
  }

  private static int parsePort(String value, String name) {
    int port = parsePositiveInt(value, name);
    if (port <= 65535) {
      return port;
    }
    throw new IllegalArgumentException(name + " must be between 1 and 65535");
  }
}
