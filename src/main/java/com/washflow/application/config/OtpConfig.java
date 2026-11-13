package com.washflow.application.config;

/**
 * The customer's WhatsApp access code (OTP_CHANNEL): "evolution" sends it through the Evolution
 * API, "log" writes it to the server log (never in production), "none" hides the option. Defaults
 * to "log" in development and "none" in production.
 */
public record OtpConfig(Channel channel, EvolutionConfig evolution) {

  public enum Channel {
    EVOLUTION,
    LOG,
    NONE
  }

  public static OtpConfig from(EnvSource env, boolean production) {
    Channel channel =
        Choices.parse(
            Channel.class,
            env.getOrDefault("OTP_CHANNEL", production ? "none" : "log"),
            "OTP_CHANNEL");
    EvolutionConfig evolution = EvolutionConfig.from(env);
    if (channel == Channel.EVOLUTION && !evolution.isConfigured()) {
      throw new ConfigError(
          "OTP_CHANNEL=evolution requires EVOLUTION_URL, EVOLUTION_API_KEY and EVOLUTION_INSTANCE");
    }
    return new OtpConfig(channel, evolution);
  }
}
