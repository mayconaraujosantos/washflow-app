package com.washflow.data.protocols;

/**
 * Deterministic keyed hash for high-entropy or short-lived secrets that must be looked up (badge
 * tokens, one-time codes). Passwords and PINs use {@link PasswordHasher} instead.
 */
public interface TokenHasher {

  String hash(String value);
}
