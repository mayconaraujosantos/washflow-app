package com.washflow.presentation.controllers.establishment;

import java.util.Map;

public final class BodyValues {

  private BodyValues() {}

  public static String text(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? null : value.toString();
  }

  public static int integer(Map<String, Object> body, String key) {
    if (body.get(key) instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalArgumentException(key + " deve ser um número");
  }
}
