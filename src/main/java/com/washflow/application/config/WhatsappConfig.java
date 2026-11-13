package com.washflow.application.config;

/**
 * Each lava-jato's own WhatsApp number (status notices and the scheduling assistant), linked by QR
 * code from the manager screen. The Evolution API (EVOLUTION_URL and EVOLUTION_API_KEY) reaches the
 * app back at {@code webhookUrl} (WHATSAPP_WEBHOOK_URL, e.g.
 * http://washflow-api:7000/api/whatsapp/webhook), sending {@code webhookToken}
 * (WHATSAPP_WEBHOOK_TOKEN) in a header. Off when neither is set.
 */
public record WhatsappConfig(String webhookUrl, String webhookToken) {

  private static final int MIN_TOKEN = 24;

  public static WhatsappConfig from(EnvSource env, EvolutionConfig evolution) {
    WhatsappConfig config =
        new WhatsappConfig(
            env.getOrDefault("WHATSAPP_WEBHOOK_URL", "").trim(),
            env.getOrDefault("WHATSAPP_WEBHOOK_TOKEN", "").trim());
    if (config.webhookUrl.isBlank() && config.webhookToken.isBlank()) {
      return config;
    }
    if (config.webhookUrl.isBlank() || config.webhookToken.length() < MIN_TOKEN) {
      throw new ConfigError(
          "WhatsApp requires WHATSAPP_WEBHOOK_URL and WHATSAPP_WEBHOOK_TOKEN with at least "
              + MIN_TOKEN
              + " characters");
    }
    if (evolution.url().isBlank() || evolution.apiKey().isBlank()) {
      throw new ConfigError("WhatsApp requires EVOLUTION_URL and EVOLUTION_API_KEY");
    }
    return config;
  }

  public boolean enabled() {
    return !webhookUrl.isBlank();
  }

  @Override
  public String toString() {
    return "Whatsapp[webhookUrl=" + webhookUrl + ", webhookToken=[redacted]]";
  }
}
