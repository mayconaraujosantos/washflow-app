package com.washflow.application.config;

/**
 * Free-text understanding for the WhatsApp assistant ("quero lavar o Onix amanhã à tarde"):
 * ANTHROPIC_API_KEY turns it on, WHATSAPP_AI_MODEL picks the Claude model. Without a key the
 * assistant still works with numbered menus.
 */
public record AssistantAiConfig(String apiKey, String model) {

  public static final String DEFAULT_MODEL = "claude-opus-5-5";

  public static AssistantAiConfig from(EnvSource env) {
    return new AssistantAiConfig(
        env.getOrDefault("ANTHROPIC_API_KEY", "").trim(),
        env.getOrDefault("WHATSAPP_AI_MODEL", DEFAULT_MODEL).trim());
  }

  public boolean enabled() {
    return !apiKey.isBlank();
  }

  @Override
  public String toString() {
    return "AssistantAi[model=" + model + ", apiKey=" + (enabled() ? "[redacted]" : "") + "]";
  }
}
