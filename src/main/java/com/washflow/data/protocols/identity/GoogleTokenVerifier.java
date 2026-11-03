package com.washflow.data.protocols.identity;

import java.util.Optional;

/** Validates a Google Identity Services ID token (signature, audience, expiry). */
public interface GoogleTokenVerifier {

  /** {@code null} when Google login is not configured. */
  String clientId();

  Optional<GoogleIdentity> verify(String idToken);

  record GoogleIdentity(String subject, String email, String name) {}
}
