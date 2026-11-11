package com.washflow.data.protocols.db.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Pending reset codes (one per account, keyed hash only) and where they are sent. */
public interface PasswordResetRepository {

  /** The customer's recovery e-mail, or the account e-mail (e.g. from Google) when unset. */
  Optional<String> loadRecoveryEmail(UUID userId);

  /** Sets (or clears, with null) the customer's recovery e-mail. */
  void saveRecoveryEmail(UUID userId, String email);

  /** Replaces any pending code of the account. */
  void saveCode(UUID userId, String codeHash, Instant expiresAt, Instant createdAt);

  Optional<PendingCode> loadCode(UUID userId);

  void incrementAttempts(UUID userId);

  void deleteCode(UUID userId);

  record PendingCode(String codeHash, int attempts, Instant expiresAt, Instant createdAt) {}
}
