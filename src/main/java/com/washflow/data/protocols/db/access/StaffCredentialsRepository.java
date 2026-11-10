package com.washflow.data.protocols.db.access;

import com.washflow.domain.entities.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StaffCredentialsRepository {

  /** Replaces the badge (PIN + QR) and clears any lockout. */
  void saveBadge(UUID userId, String pinHash, String badgeHash);

  Optional<PinState> loadPinState(UUID userId);

  void recordPinFailure(UUID userId, int failedAttempts, Instant lockedUntil);

  void resetPinFailures(UUID userId);

  Optional<User> loadByBadgeHash(String badgeHash);

  record PinState(String pinHash, int failedAttempts, Instant lockedUntil) {}
}
