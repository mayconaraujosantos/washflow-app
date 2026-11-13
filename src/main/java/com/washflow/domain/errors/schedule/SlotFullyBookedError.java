package com.washflow.domain.errors.schedule;

import com.washflow.domain.errors.common.ConflictError;
import java.time.Instant;

/**
 * The hour-long slot containing {@code scheduledAt} already has {@code maxVehiclesPerHour} service
 * orders booked - {@code docs/feature.spec.md}'s MVP scheduling rules cap yard capacity per hour
 * (default 3, configurable).
 */
public class SlotFullyBookedError extends ConflictError {

  public SlotFullyBookedError(Instant scheduledAt, int maxVehiclesPerHour) {
    super(
        "SLOT_FULLY_BOOKED",
        "Time slot for "
            + scheduledAt
            + " is fully booked (max "
            + maxVehiclesPerHour
            + " vehicles per hour)");
  }
}
