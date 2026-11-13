package com.washflow.domain.entities.whatsapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts between the app's phones (DDD + number, 10 or 11 digits, see ContactRules) and WhatsApp
 * numbers (country code + number). Older Brazilian mobile accounts are registered on WhatsApp
 * without the ninth digit, so a sender may need both forms to match an account.
 */
public final class WhatsappNumber {

  private static final String BRAZIL = "55";

  private WhatsappNumber() {}

  /** The WhatsApp number to send to: the app's phone with the Brazil country code. */
  public static String of(String phone) {
    String digits = phone.replaceAll("\\D", "");
    return digits.length() <= 11 ? BRAZIL + digits : digits;
  }

  /**
   * The digits of a sender JID ({@code 5592991234567@s.whatsapp.net}); empty for groups, broadcast
   * lists and anything that isn't a phone.
   */
  public static Optional<String> fromJid(String jid) {
    if (jid == null || !jid.endsWith("@s.whatsapp.net")) {
      return Optional.empty();
    }
    String digits = jid.substring(0, jid.indexOf('@')).replaceAll("\\D", "");
    return digits.length() >= 10 ? Optional.of(digits) : Optional.empty();
  }

  /**
   * App phones a Brazilian WhatsApp number may belong to: as written, and with or without the
   * mobile ninth digit. Empty for numbers outside Brazil.
   */
  public static List<String> phoneCandidates(String whatsappNumber) {
    if (!whatsappNumber.startsWith(BRAZIL)) {
      return List.of();
    }
    String national = whatsappNumber.substring(BRAZIL.length());
    List<String> candidates = new ArrayList<>();
    if (national.length() == 11 || national.length() == 10) {
      candidates.add(national);
    }
    if (national.length() == 10 && national.charAt(2) >= '6') {
      candidates.add(national.substring(0, 2) + "9" + national.substring(2));
    }
    if (national.length() == 11 && national.charAt(2) == '9') {
      candidates.add(national.substring(0, 2) + national.substring(3));
    }
    return List.copyOf(candidates);
  }
}
