package com.washflow.application.audit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Persistence of the audit trail ({@link AuditTrail}); rows are never changed or deleted. */
public interface AuditStore {

  /** One successful change: who, where, what was sent (secrets masked) and when. */
  record Entry(
      UUID establishmentId,
      UUID userId,
      String profile,
      String method,
      String route,
      String path,
      int status,
      String details,
      Instant at) {}

  /** An entry as the manager reads it, with the author's current name. */
  record Row(
      Instant at,
      UUID userId,
      String userName,
      String profile,
      String method,
      String route,
      String path,
      int status,
      String details) {}

  void save(Entry entry);

  /** The current lava-jato's entries between two Manaus dates (inclusive), newest first. */
  List<Row> list(LocalDate from, LocalDate to);
}
