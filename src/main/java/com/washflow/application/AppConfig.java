package com.washflow.application;

import com.washflow.application.config.AppConfigLoader;
import com.washflow.application.config.AppEnvironment;
import com.washflow.application.config.CompositeEnvSource;
import com.washflow.application.config.DatabaseConfig;
import com.washflow.application.config.EmailConfig;
import com.washflow.application.config.OtpConfig;
import com.washflow.application.config.TokenSecret;
import java.util.Map;

/**
 * Immutable application configuration, resolved once at startup. How each setting is read and
 * validated lives in {@code application.config}; this record only holds the result.
 */
public record AppConfig(
    int port,
    String environment,
    DatabaseConfig database,
    TokenSecret tokenSecret,
    int maxVehiclesPerHour,
    String bootstrapManagerPhone,
    String bootstrapManagerPassword,
    String setupCode,
    OtpConfig otp,
    EmailConfig email,
    String googleClientId,
    String publicAppUrl,
    String metricsToken) {

  /** Process environment first, then the .env file (ignored in production). */
  public static AppConfig load() {
    return new AppConfigLoader(CompositeEnvSource.fromEnvironmentAndDotenv()).load();
  }

  /** Same rules as {@link #load()} with explicit values (tests). */
  public static AppConfig load(Map<String, String> processEnv, Map<String, String> dotenvValues) {
    return new AppConfigLoader(CompositeEnvSource.of(processEnv, dotenvValues)).load();
  }

  public boolean isProduction() {
    return AppEnvironment.isProduction(environment);
  }

  public boolean canClaimUnconfiguredUsers() {
    return !isProduction();
  }

  @Override
  public String toString() {
    return "AppConfig[port="
        + port
        + ", environment="
        + environment
        + ", database="
        + database
        + ", tokenSecret="
        + tokenSecret
        + ", maxVehiclesPerHour="
        + maxVehiclesPerHour
        + ", bootstrapManagerPassword=[redacted]"
        + ", otp="
        + otp
        + ", email="
        + email
        + "]";
  }
}
