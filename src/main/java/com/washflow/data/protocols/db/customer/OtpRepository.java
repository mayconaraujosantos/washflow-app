package com.washflow.data.protocols.db.customer;

import com.washflow.domain.entities.customer.OtpRecipient;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository {

  /** Replaces any pending code for the recipient. */
  void save(OtpRecipient recipient, String codeHash, Instant expiresAt, Instant createdAt);

  Optional<PendingCode> loadPending(OtpRecipient recipient);

  void incrementAttempts(UUID codeId);

  void delete(OtpRecipient recipient);

  record PendingCode(
      UUID id, String codeHash, int attempts, Instant expiresAt, Instant createdAt) {}
}
