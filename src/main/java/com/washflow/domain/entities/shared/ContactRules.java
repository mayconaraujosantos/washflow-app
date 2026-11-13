package com.washflow.domain.entities.shared;

import java.util.regex.Pattern;

/** Normalization shared by the counter forms (vehicle entry, quick sale, budget). */
public final class ContactRules {

  private static final Pattern PHONE = Pattern.compile("^\\d{10,11}$");
  // local@label.label.tld - labels can't be empty or contain dots, so each character has one
  // place to match and the check is linear (no backtracking).
  private static final Pattern EMAIL =
      Pattern.compile("^[^@\\s]{1,64}@(?:[^@\\s.]+\\.){1,8}[^@\\s.]{2,}$");
  private static final int MAX_EMAIL = 254;

  private ContactRules() {}

  /**
   * The one way a phone is stored and looked up: digits only, without the +55 country code, and a
   * mobile always with its ninth digit ({@code 9286061046} and {@code +55 92 98606-1046} are both
   * {@code 92986061046}). Two spellings of the same number are the same account. {@code null} when
   * blank.
   */
  public static String normalizePhone(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String digits = canonical(raw);
    if (!PHONE.matcher(digits).matches()) {
      throw new IllegalArgumentException("Telefone deve ter DDD + número (10 ou 11 dígitos)");
    }
    return digits;
  }

  /**
   * {@link #normalizePhone} for sign-in lookups: a phone that isn't valid comes back as digits (it
   * matches no account and the sign-in fails as wrong credentials) instead of a 400.
   */
  public static String lookupPhone(String raw) {
    return raw == null || raw.isBlank() ? raw : canonical(raw);
  }

  private static String canonical(String raw) {
    String digits = raw.replaceAll("\\D", "");
    if (digits.startsWith("55") && digits.length() > 11) {
      digits = digits.substring(2);
    }
    // Brazilian mobiles (subscriber starting with 6-9) gained the ninth digit; landlines (2-5)
    // didn't.
    if (digits.length() == 10 && digits.charAt(2) >= '6') {
      digits = digits.substring(0, 2) + "9" + digits.substring(2);
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
