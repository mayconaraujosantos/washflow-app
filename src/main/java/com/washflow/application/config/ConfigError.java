package com.washflow.application.config;

/**
 * A missing or invalid setting: the app refuses to start with the setting's name in the message.
 */
public class ConfigError extends RuntimeException {

  public ConfigError(String message) {
    super(message);
  }
}
