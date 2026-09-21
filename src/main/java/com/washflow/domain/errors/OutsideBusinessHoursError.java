package com.washflow.domain.errors;

import java.time.Instant;

/**
 * The requested {@code scheduledAt} falls outside the shop's business hours (Monday-Saturday,
 * 08:00-18:00, per {@code docs/feature.spec.md}'s MVP scheduling rules).
 */
public class OutsideBusinessHoursError extends Exception {

  public OutsideBusinessHoursError(Instant scheduledAt) {
    super(
        "Scheduled time "
            + scheduledAt
            + " is outside business hours (Monday-Saturday, 08:00-18:00)");
  }
}
