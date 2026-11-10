package com.washflow.presentation.helpers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Typed reads of a JSON request body (the {@code Map} built by the route adapter). Each accessor
 * returns the value in its type, or {@code null} when the field is absent, and throws {@link
 * IllegalArgumentException} - a 400 in every controller - when the client sent another JSON type,
 * instead of a {@code ClassCastException} that would surface as a 500.
 */
public final class BodyValues {

  private BodyValues() {}

  public static String text(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null || value instanceof String) {
      return (String) value;
    }
    throw new IllegalArgumentException(key + " deve ser um texto");
  }

  public static int integer(Map<String, Object> body, String key) {
    if (body.get(key) instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalArgumentException(key + " deve ser um número");
  }

  public static BigDecimal decimal(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return new BigDecimal(number.toString());
    }
    throw new IllegalArgumentException(key + " deve ser um número");
  }

  public static Boolean bool(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null || value instanceof Boolean) {
      return (Boolean) value;
    }
    throw new IllegalArgumentException(key + " deve ser verdadeiro ou falso");
  }

  /** A UUID sent as text; blank counts as absent. */
  public static UUID uuid(Map<String, Object> body, String key) {
    String value = text(body, key);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(key + " inválido", e);
    }
  }

  /** A list of JSON objects (e.g. sale items); absent is an empty list. */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> objects(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> list && list.stream().allMatch(Map.class::isInstance)) {
      return (List<Map<String, Object>>) list;
    }
    throw new IllegalArgumentException(key + " deve ser uma lista de objetos");
  }

  /** The authenticated user's id, injected as {@code createdBy} by the route adapter. */
  public static UUID createdBy(Map<String, Object> body) {
    return body.get("createdBy") instanceof UUID user ? user : null;
  }
}
