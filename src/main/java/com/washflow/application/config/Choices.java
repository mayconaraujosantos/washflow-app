package com.washflow.application.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Parses a setting into one of an enum's constants, case-insensitive. */
final class Choices {

  private Choices() {}

  static <E extends Enum<E>> E parse(Class<E> type, String raw, String key) {
    String value = raw.trim();
    return Arrays.stream(type.getEnumConstants())
        .filter(constant -> constant.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new ConfigError(key + " must be " + options(type)));
  }

  static String lower(Enum<?> constant) {
    return constant.name().toLowerCase(Locale.ROOT);
  }

  private static <E extends Enum<E>> String options(Class<E> type) {
    E[] constants = type.getEnumConstants();
    String head =
        Arrays.stream(constants, 0, constants.length - 1)
            .map(Choices::lower)
            .collect(Collectors.joining(", "));
    return head + " or " + lower(constants[constants.length - 1]);
  }
}
