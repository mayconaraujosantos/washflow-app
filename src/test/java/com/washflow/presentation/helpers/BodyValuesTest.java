package com.washflow.presentation.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BodyValuesTest {

  private static Map<String, Object> body(Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return body;
  }

  @Test
  void readsTextAndRefusesOtherJsonTypes() {
    assertEquals("Chuva", BodyValues.text(body("reason", "Chuva"), "reason"));
    assertNull(BodyValues.text(body(), "reason"));

    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class, () -> BodyValues.text(body("reason", 123), "reason"));
    assertEquals("reason deve ser um texto", error.getMessage());
  }

  @Test
  void readsBooleansStrictly() {
    assertEquals(true, BodyValues.bool(body("active", true), "active"));
    assertNull(BodyValues.bool(body(), "active"));
    assertThrows(
        IllegalArgumentException.class, () -> BodyValues.bool(body("active", "sim"), "active"));
  }

  @Test
  void readsNumbersAsIntegerOrDecimal() {
    assertEquals(7, BodyValues.integer(body("hour", 7), "hour"));
    assertThrows(IllegalArgumentException.class, () -> BodyValues.integer(body(), "hour"));
    assertEquals(new BigDecimal("12.5"), BodyValues.decimal(body("amount", 12.5), "amount"));
    assertNull(BodyValues.decimal(body(), "amount"));
    assertThrows(
        IllegalArgumentException.class, () -> BodyValues.decimal(body("amount", "dez"), "amount"));
  }

  @Test
  void readsUuidsFromText() {
    UUID id = UUID.randomUUID();
    assertEquals(id, BodyValues.uuid(body("vehicleId", id.toString()), "vehicleId"));
    assertNull(BodyValues.uuid(body("vehicleId", " "), "vehicleId"));
    assertThrows(
        IllegalArgumentException.class, () -> BodyValues.uuid(body("vehicleId", "x"), "vehicleId"));
  }

  @Test
  void readsListsOfObjects() {
    Map<String, Object> item = Map.of("servicePriceId", 1);
    assertEquals(List.of(item), BodyValues.objects(body("items", List.of(item)), "items"));
    assertEquals(List.of(), BodyValues.objects(body(), "items"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BodyValues.objects(body("items", List.of("x")), "items"));
  }

  @Test
  void readsListsOfIntegersWithoutTruncatingDecimalValues() {
    assertEquals(List.of(1, 2), BodyValues.integers(body("ids", List.of(1, 2)), "ids"));
    assertEquals(List.of(), BodyValues.integers(body(), "ids"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BodyValues.integers(body("ids", List.of(1, "2")), "ids"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BodyValues.integers(body("ids", List.of(1, 2.5)), "ids"));
  }

  @Test
  void readsTheAuthenticatedUserInjectedByTheRouteAdapter() {
    UUID user = UUID.randomUUID();
    assertEquals(user, BodyValues.createdBy(body("createdBy", user)));
    assertNull(BodyValues.createdBy(body()));
  }
}
