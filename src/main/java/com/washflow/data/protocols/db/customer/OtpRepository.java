package com.washflow.data.protocols.db.customer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository {

  /** Replaces any pending code for the phone. */
  void save(String phone, String codeHash, Instant expiresAt, Instant createdAt);

  Optional<PendingCode> loadPending(String phone);

  void incrementAttempts(UUID codeId);

  void delete(String phone);

  record PendingCode(
      UUID id, String codeHash, int attempts, Instant expiresAt, Instant createdAt) {}
}
