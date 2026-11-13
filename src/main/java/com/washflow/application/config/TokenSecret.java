package com.washflow.application.config;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * HMAC key of the session tokens (AUTH_TOKEN_SECRET). Production requires 32+ characters;
 * development without one gets a random key, so tokens die with the process.
 */
public record TokenSecret(String value) {

  private static final int MIN_LENGTH = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  public static TokenSecret from(EnvSource env, boolean production) {
    String raw = env.getOrDefault("AUTH_TOKEN_SECRET", "");
    if (production && raw.length() < MIN_LENGTH) {
      throw new ConfigError(
          "Production requires AUTH_TOKEN_SECRET with at least " + MIN_LENGTH + " characters");
    }
    return new TokenSecret(raw.isBlank() ? randomSecret() : raw);
  }

  private static String randomSecret() {
    byte[] secret = new byte[MIN_LENGTH];
    RANDOM.nextBytes(secret);
    return Base64.getEncoder().encodeToString(secret);
  }

  @Override
  public String toString() {
    return "[redacted]";
  }
}
