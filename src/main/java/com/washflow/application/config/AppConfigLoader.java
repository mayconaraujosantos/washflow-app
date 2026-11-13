package com.washflow.application.config;

import com.washflow.application.AppConfig;

/** Reads every setting from an {@link EnvSource} and fails fast on the first invalid one. */
public final class AppConfigLoader {

  private final EnvSource env;

  public AppConfigLoader(EnvSource env) {
    this.env = env;
  }

  public AppConfig load() {
    String environment = env.getOrDefault("APP_ENV", AppEnvironment.DEVELOPMENT);
    boolean production = AppEnvironment.isProduction(environment);
    OtpConfig otp = OtpConfig.from(env, production);
    return new AppConfig(
        Ports.parse(env.getOrDefault("PORT", "7000"), "PORT"),
        environment,
        DatabaseConfig.from(env, production),
        TokenSecret.from(env, production),
        Ports.parsePositive(
            env.getOrDefault("MAX_VEHICLES_PER_HOUR", "3"), "MAX_VEHICLES_PER_HOUR"),
        env.getOrDefault("BOOTSTRAP_MANAGER_PHONE", "11999993333"),
        env.getOrDefault("BOOTSTRAP_MANAGER_PASSWORD", ""),
        // Owner-only secret that unlocks the first-run setup wizard in production.
        env.getOrDefault("SETUP_CODE", ""),
        otp,
        EmailConfig.from(env, production),
        // Google Identity Services client ID; empty hides "Entrar com Google".
        env.getOrDefault("GOOGLE_CLIENT_ID", ""),
        // Public origin of the magic links.
        env.getOrDefault("APP_PUBLIC_URL", ""),
        // Bearer token for GET /api/metrics; empty keeps the endpoint off.
        env.getOrDefault("METRICS_TOKEN", ""),
        WhatsappConfig.from(env, otp.evolution()),
        AssistantAiConfig.from(env));
  }
}
