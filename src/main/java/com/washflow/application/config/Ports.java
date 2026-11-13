package com.washflow.application.config;

/** Integer settings parsed fail-fast, with the setting's name in the error. */
public final class Ports {

  private static final int MAX_PORT = 65535;

  private Ports() {}

  public static int parse(String raw, String key) {
    int port = parsePositive(raw, key);
    if (port > MAX_PORT) {
      throw new ConfigError(key + " must be between 1 and " + MAX_PORT);
    }
    return port;
  }

  public static int parsePositive(String raw, String key) {
    try {
      int value = Integer.parseInt(raw.trim());
      if (value > 0) {
        return value;
      }
    } catch (NumberFormatException ignored) {
      // Report a consistent configuration error below.
    }
    throw new ConfigError(key + " must be a positive integer");
  }
}
