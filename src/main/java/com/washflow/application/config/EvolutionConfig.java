package com.washflow.application.config;

/**
 * WhatsApp delivery through a self-hosted Evolution API (OTP_CHANNEL=evolution): {@code url} is the
 * server base URL, {@code apiKey} its global AUTHENTICATION_API_KEY and {@code instance} the
 * connected WhatsApp number's instance name.
 */
public record EvolutionConfig(String url, String apiKey, String instance) {

  public static EvolutionConfig from(EnvSource env) {
    return new EvolutionConfig(
        env.getOrDefault("EVOLUTION_URL", "").trim(),
        env.getOrDefault("EVOLUTION_API_KEY", "").trim(),
        env.getOrDefault("EVOLUTION_INSTANCE", "").trim());
  }

  public boolean isConfigured() {
    return !url.isBlank() && !apiKey.isBlank() && !instance.isBlank();
  }

  @Override
  public String toString() {
    return "Evolution[url=" + url + ", instance=" + instance + ", apiKey=[redacted]]";
  }
}
