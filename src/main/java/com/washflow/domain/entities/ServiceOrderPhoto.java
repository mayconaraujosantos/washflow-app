package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

public record ServiceOrderPhoto(
    UUID id,
    UUID serviceOrderId,
    PhotoType type,
    String imageData,
    String description,
    Instant createdAt,
    UUID createdBy) {

  public enum PhotoType {
    ENTRY,
    EXIT
  }
}
