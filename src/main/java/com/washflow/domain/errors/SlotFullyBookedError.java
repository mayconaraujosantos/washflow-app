package com.washflow.domain.errors;

import java.time.Instant;

/**
 * The hour-long slot containing {@code scheduledAt} already has {@code maxVehiclesPerHour} service
 * orders booked - {@code docs/feature.spec.md}'s MVP scheduling rules cap yard capacity per hour
 * (default 3, configurable).
 */
public class SlotFullyBookedError extends Exception {

  public SlotFullyBookedError(Instant scheduledAt, int maxVehiclesPerHour) {
    super(
        "Time slot for "
            + scheduledAt
            + " is fully booked (max "
            + maxVehiclesPerHour
            + " vehicles per hour)");
  }
}
