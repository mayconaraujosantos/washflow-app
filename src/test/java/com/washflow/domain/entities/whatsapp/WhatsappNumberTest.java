package com.washflow.domain.entities.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsappNumberTest {

  @Test
  void addsTheBrazilCountryCodeOnlyWhenMissing() {
    assertEquals("559233334444", WhatsappNumber.of("9233334444"));
    assertEquals("5592991234567", WhatsappNumber.of("92991234567"));
    assertEquals("5592991234567", WhatsappNumber.of("+55 (92) 99123-4567"));
  }

  @Test
  void readsOnlyPhoneSenders() {
    assertEquals(
        Optional.of("5592991234567"), WhatsappNumber.fromJid("5592991234567@s.whatsapp.net"));
    assertTrue(WhatsappNumber.fromJid("120363025246125486@g.us").isEmpty());
    assertTrue(WhatsappNumber.fromJid("status@broadcast").isEmpty());
    assertTrue(WhatsappNumber.fromJid(null).isEmpty());
  }

  @Test
  void matchesAccountsWithAndWithoutTheNinthDigit() {
    assertEquals(
        List.of("9291234567", "92991234567"), WhatsappNumber.phoneCandidates("559291234567"));
    assertEquals(
        List.of("92991234567", "9291234567"), WhatsappNumber.phoneCandidates("5592991234567"));
    // Landline: no ninth digit to add.
    assertEquals(List.of("9233334444"), WhatsappNumber.phoneCandidates("559233334444"));
    assertEquals(List.of(), WhatsappNumber.phoneCandidates("14155550123"));
  }

  @Test
  void instanceNamesCarryTheEstablishment() {
    UUID id = UUID.randomUUID();

    assertEquals(Optional.of(id), WhatsappInstance.establishmentOf(WhatsappInstance.nameOf(id)));
    assertTrue(WhatsappInstance.establishmentOf("washflow").isEmpty());
    assertTrue(WhatsappInstance.establishmentOf("washflow-not-a-uuid").isEmpty());
  }
}
