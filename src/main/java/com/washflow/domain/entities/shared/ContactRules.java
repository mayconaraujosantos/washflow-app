package com.washflow.domain.entities.shared;

import java.util.regex.Pattern;

/** Normalization shared by the counter forms (vehicle entry, quick sale, budget). */
public final class ContactRules {

  private static final Pattern PHONE = Pattern.compile("^\\d{10,11}$");
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");
  private static final int MAX_EMAIL = 254;

  private ContactRules() {}

  /** Digits only, without the +55 country code; {@code null} when blank. */
  public static String normalizePhone(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.startsWith("55") && digits.length() > 11) {
      digits = digits.substring(2);
    }
    if (!PHONE.matcher(digits).matches()) {
      throw new IllegalArgumentException("Telefone deve ter DDD + número (10 ou 11 dígitos)");
    }
    return digits;
  }

  /** Lower-cased e-mail, {@code null} when blank; rejects anything not shaped like one. */
  public static String normalizeEmail(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String email = raw.trim().toLowerCase(java.util.Locale.ROOT);
    if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches()) {
      throw new IllegalArgumentException("E-mail inválido");
    }
    return email;
  }

  /** Trimmed text, {@code null} when blank, rejected when longer than {@code max}. */
  public static String limited(String value, int max, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() > max) {
      throw new IllegalArgumentException(field + " deve ter no máximo " + max + " caracteres");
    }
    return trimmed;
  }
}
