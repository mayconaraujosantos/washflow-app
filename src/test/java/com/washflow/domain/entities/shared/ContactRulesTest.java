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

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "92986061046",
        "+55 92 98606-1046",
        "559286061046",
        "(92) 8606-1046",
        "5592986061046",
        "92 98606 1046"
      })
  void everySpellingOfAMobileIsTheSameNumber(String phone) {
    assertEquals("92986061046", ContactRules.normalizePhone(phone));
  }

  @org.junit.jupiter.api.Test
  void landlinesKeepTenDigits() {
    assertEquals("9233334444", ContactRules.normalizePhone("+55 92 3333-4444"));
    assertEquals("1155554444", ContactRules.normalizePhone("1155554444"));
  }

  @org.junit.jupiter.api.Test
  void aSignInLookupNeverRejectsTheInput() {
    assertEquals("92986061046", ContactRules.lookupPhone("559286061046"));
    // Too short to be a phone: no account matches it, so the sign-in just fails.
    assertEquals("123", ContactRules.lookupPhone("1-2-3"));
    assertThrows(IllegalArgumentException.class, () -> ContactRules.normalizePhone("1-2-3"));
  }
}
