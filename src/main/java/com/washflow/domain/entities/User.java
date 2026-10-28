package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code usuarios} from CLAUDE.md. The persisted password hash is deliberately excluded from this
 * public model; authentication loads it through a dedicated repository port.
 */
public record User(
    UUID id, String name, String phone, UserProfile profile, Instant createdAt, boolean active) {

  public User(UUID id, String name, String phone, UserProfile profile, Instant createdAt) {
    this(id, name, phone, profile, createdAt, true);
  }
}
