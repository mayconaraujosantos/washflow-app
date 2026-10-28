package com.washflow.domain.errors;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The requested {@code scheduledAt} falls outside the shop's business hours in Manaus
 * (Monday-Saturday, 08:00-18:00, per {@code docs/feature.spec.md}'s MVP scheduling rules).
 */
public class OutsideBusinessHoursError extends Exception {

  private static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");
  private static final DateTimeFormatter LOCAL_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public OutsideBusinessHoursError(Instant scheduledAt) {
    super(
        "Scheduled time "
            + LOCAL_TIME.format(scheduledAt.atZone(STORE_ZONE))
            + " Manaus time is outside business hours (Monday-Saturday, 08:00-18:00)");
  }
}
