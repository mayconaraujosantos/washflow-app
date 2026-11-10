package com.washflow.data.usecases.access;

import java.security.SecureRandom;
import java.util.Base64;

/** Random PINs, codes and tokens for the access flows (washer badge and customer codes/tickets). */
public final class Secrets {

  private static final SecureRandom RANDOM = new SecureRandom();

  private Secrets() {}

  public static String digits(int length) {
    StringBuilder value = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      value.append(RANDOM.nextInt(10));
    }
    return value.toString();
  }

  /** URL-safe token with {@code bytes} bytes of entropy. */
  public static String token(int bytes) {
    byte[] raw = new byte[bytes];
    RANDOM.nextBytes(raw);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }
}
