package com.washflow.data.usecases.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DbManagePlatformSlugTest {

  @Test
  void turnsANameIntoAnAddress() {
    assertEquals("lava-rapido-sao-jose", DbManagePlatform.slug("Lava Rápido São José"));
    assertEquals("lava-24h", DbManagePlatform.slug("  --Lava 24h!!  "));
  }

  @Test
  void truncatesWithoutATrailingDash() {
    String slug = DbManagePlatform.slug("a".repeat(39) + " bcd");
    assertEquals("a".repeat(39), slug);
  }

  @Test
  void refusesNamesWithoutLettersOrDigits() {
    assertThrows(IllegalArgumentException.class, () -> DbManagePlatform.slug("!!"));
  }

  @Test
  void handlesLongPunctuationInLinearTime() {
    assertTimeoutPreemptively(
        Duration.ofMillis(200),
        () -> assertEquals("a-b", DbManagePlatform.slug("a" + "-!".repeat(50_000) + "b")));
  }
}
