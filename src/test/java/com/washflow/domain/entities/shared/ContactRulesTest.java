package com.washflow.domain.entities.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContactRulesTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"ana@lava.com", "Ana.Souza+caixa@sub.lava.com.br", "a@b.co", "x@lava-jato.io"})
  void acceptsUsualEmailsInLowercase(String email) {
    assertEquals(
        email.toLowerCase(java.util.Locale.ROOT), ContactRules.normalizeEmail(" " + email + " "));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@lava.com",
        "ana@lava",
        "ana@lava.c",
        "a b@lava.com",
        "a@@lava.com",
        "a@lava..com",
        "a@.lava.com"
      })
  void rejectsMalformedEmails(String email) {
    assertThrows(IllegalArgumentException.class, () -> ContactRules.normalizeEmail(email));
  }

  @Test
  void blankEmailIsAbsent() {
    assertNull(ContactRules.normalizeEmail("  "));
  }

  @Test
  void validatesALongHostileDomainInLinearTime() {
    String hostile = "a@" + "b.".repeat(120) + "c";
    assertTimeoutPreemptively(
        Duration.ofMillis(200),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> ContactRules.normalizeEmail(hostile)));
  }
}
