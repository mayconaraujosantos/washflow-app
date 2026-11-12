package com.washflow.domain.errors.schedule;

import com.washflow.domain.errors.common.ValidationError;
import java.time.Duration;
import java.time.Instant;

/**
 * The requested {@code scheduledAt} is too close to now - {@code docs/feature.spec.md}'s MVP
 * scheduling rules require at least 30 minutes of lead time.
 */
public class InsufficientLeadTimeError extends ValidationError {

  public InsufficientLeadTimeError(Instant scheduledAt, Duration minLeadTime) {
    super(
        "INSUFFICIENT_LEAD_TIME",
        "Scheduled time "
            + scheduledAt
            + " does not meet the minimum lead time of "
            + minLeadTime.toMinutes()
            + " minutes");
  }
}
