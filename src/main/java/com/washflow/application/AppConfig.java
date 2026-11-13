package com.washflow.application;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    String bootstrapManagerPassword,
    String setupCode,
    String otpChannel,
    String googleClientId,
    String publicAppUrl,
    String metricsToken,
    String emailChannel,
    Smtp smtp,
    Evolution evolution) {

  /**
   * WhatsApp delivery through a self-hosted Evolution API (OTP_CHANNEL=evolution): {@code url} is
   * the server base URL, {@code apiKey} its global AUTHENTICATION_API_KEY and {@code instance} the
   * connected WhatsApp number's instance name.
   */
  public record Evolution(String url, String apiKey, String instance) {

    @Override
    public String toString() {
      return "Evolution[url=" + url + ", instance=" + instance + ", apiKey=[redacted]]";
    }
  }

  /**
   * Outgoing e-mail: customer access codes and password-reset codes share it. {@code emailChannel}
   * is "smtp", "log" (development only) or "none"; when EMAIL_CHANNEL is not set it is "smtp" as
   * soon as SMTP_HOST is, otherwise "log" in development and "none" in production. Works with any
   * SMTP relay (Brevo, Resend, Amazon SES, Gmail app password; Mailpit locally).
   *
   * @param security "ssl" (implicit TLS, port 465), "starttls" (587) or "none" (local catcher)
   */
  public record Smtp(
      String host,
      int port,
      String username,
      String password,
      String security,
      String from,
      String fromName) {

    @Override
    public String toString() {
      return "Smtp[host="
          + host
          + ", port="
          + port
          + ", security="
          + security
          + ", from="
          + from
          + ", password=[redacted]]";
    }
  }

  private static final int DEFAULT_PORT = 7000;
  private static final int DEFAULT_MAX_VEHICLES_PER_HOUR = 3;
  public static final String WASHFLOW = "washflow";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
      SECURE_RANDOM.nextBytes(secret);
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
            value(processEnv, dotenvValues, useDotenv, "DB_NAME", WASHFLOW),
            value(processEnv, dotenvValues, useDotenv, "DB_USER", WASHFLOW),
            value(processEnv, dotenvValues, useDotenv, "DB_PASSWORD", WASHFLOW),
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
            value(processEnv, dotenvValues, useDotenv, "BOOTSTRAP_MANAGER_PASSWORD", ""),
            // Owner-only secret that unlocks the first-run setup wizard in production.
            value(processEnv, dotenvValues, useDotenv, "SETUP_CODE", ""),
            // WhatsApp code delivery: "evolution", "log" (development only) or "none".
            value(
                    processEnv,
                    dotenvValues,
                    useDotenv,
                    "OTP_CHANNEL",
                    isProduction(environment) ? "none" : "log")
                .trim()
                .toLowerCase(Locale.ROOT),
            value(processEnv, dotenvValues, useDotenv, "GOOGLE_CLIENT_ID", ""),
            value(processEnv, dotenvValues, useDotenv, "APP_PUBLIC_URL", ""),
            // Bearer token for GET /api/metrics; empty keeps the endpoint off.
            value(processEnv, dotenvValues, useDotenv, "METRICS_TOKEN", ""),
            emailChannel(processEnv, dotenvValues, useDotenv, environment),
            new Smtp(
                value(processEnv, dotenvValues, useDotenv, "SMTP_HOST", ""),
                parsePort(
                    value(processEnv, dotenvValues, useDotenv, "SMTP_PORT", "587"), "SMTP_PORT"),
                value(processEnv, dotenvValues, useDotenv, "SMTP_USERNAME", ""),
                value(processEnv, dotenvValues, useDotenv, "SMTP_PASSWORD", ""),
                value(processEnv, dotenvValues, useDotenv, "SMTP_SECURITY", "starttls")
                    .trim()
                    .toLowerCase(Locale.ROOT),
                value(processEnv, dotenvValues, useDotenv, "SMTP_FROM", ""),
                value(processEnv, dotenvValues, useDotenv, "SMTP_FROM_NAME", "Washflow")),
            new Evolution(
                value(processEnv, dotenvValues, useDotenv, "EVOLUTION_URL", "").trim(),
                value(processEnv, dotenvValues, useDotenv, "EVOLUTION_API_KEY", "").trim(),
                value(processEnv, dotenvValues, useDotenv, "EVOLUTION_INSTANCE", "").trim()));

    if (!Set.of("evolution", "log", "none").contains(config.otpChannel())) {
      throw new IllegalStateException("OTP_CHANNEL must be evolution, log or none");
    }
    if ("evolution".equals(config.otpChannel())
        && (config.evolution().url().isBlank()
            || config.evolution().apiKey().isBlank()
            || config.evolution().instance().isBlank())) {
      throw new IllegalStateException(
          "OTP_CHANNEL=evolution requires EVOLUTION_URL, EVOLUTION_API_KEY and EVOLUTION_INSTANCE");
    }

    if (!Set.of("smtp", "log", "none").contains(config.emailChannel())) {
      throw new IllegalStateException("EMAIL_CHANNEL must be smtp, log or none");
    }
    if (!Set.of("ssl", "starttls", "none").contains(config.smtp().security())) {
      throw new IllegalStateException("SMTP_SECURITY must be ssl, starttls or none");
    }
    if ("smtp".equals(config.emailChannel())
        && (config.smtp().host().isBlank() || config.smtp().from().isBlank())) {
      throw new IllegalStateException("EMAIL_CHANNEL=smtp requires SMTP_HOST and SMTP_FROM");
    }

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
        + ", emailChannel="
        + emailChannel
        + ", smtp="
        + smtp
        + ", otpChannel="
        + otpChannel
        + ", evolution="
        + evolution
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

  private static String emailChannel(
      Map<String, String> processEnv,
      Map<String, String> dotenvValues,
      boolean useDotenv,
      String environment) {
    String fallback;
    if (!value(processEnv, dotenvValues, useDotenv, "SMTP_HOST", "").isBlank()) {
      fallback = "smtp";
    } else {
      fallback = isProduction(environment) ? "none" : "log";
    }
    return value(processEnv, dotenvValues, useDotenv, "EMAIL_CHANNEL", fallback)
        .trim()
        .toLowerCase(Locale.ROOT);
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
