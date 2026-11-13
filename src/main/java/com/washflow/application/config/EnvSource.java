package com.washflow.application.config;

import java.util.Optional;

/** Where settings come from (process environment, .env file, a map in tests). */
@FunctionalInterface
public interface EnvSource {

  /** The value of {@code key}; empty when unset or blank. */
  Optional<String> get(String key);

  default String getOrDefault(String key, String fallback) {
    return get(key).filter(value -> !value.isBlank()).orElse(fallback);
  }

  default boolean isBlank(String key) {
    return get(key).filter(value -> !value.isBlank()).isEmpty();
  }
}
