package com.washflow.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServiceOrderChecklist(
    UUID id,
    UUID serviceOrderId,
    ChecklistType type,
    List<ChecklistItem> items,
    String signatureImage,
    Instant createdAt,
    UUID createdBy) {

  public record ChecklistItem(String label, boolean checked, String note) {}

  public enum ChecklistType {
    ENTRY,
    EXIT
  }
}
