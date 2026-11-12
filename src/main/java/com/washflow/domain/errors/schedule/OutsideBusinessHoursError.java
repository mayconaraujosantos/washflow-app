package com.washflow.domain.errors.schedule;

import com.washflow.domain.errors.common.ValidationError;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The requested {@code scheduledAt} falls outside the shop's business hours in Manaus
 * (Monday-Saturday, 08:00-18:00, per {@code docs/feature.spec.md}'s MVP scheduling rules).
 */
public class OutsideBusinessHoursError extends ValidationError {

  private static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");
  private static final DateTimeFormatter LOCAL_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public OutsideBusinessHoursError(Instant scheduledAt) {
    this(scheduledAt, 8, 18, false);
  }

  public OutsideBusinessHoursError(
      Instant scheduledAt, int openingHour, int closingHour, boolean openOnSunday) {
    super(
        "OUTSIDE_BUSINESS_HOURS",
        "Scheduled time "
            + LOCAL_TIME.format(scheduledAt.atZone(STORE_ZONE))
            + " Manaus time is outside business hours ("
            + (openOnSunday ? "every day" : "Monday-Saturday")
            + String.format(", %02d:00-%02d:00)", openingHour, closingHour));
  }
}
