package com.washflow.data.usecases.scheduleserviceorder;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.SlotFullyBookedError;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The scheduling rules from {@code docs/feature.spec.md} 1.4, shared by customer self-scheduling
 * ({@link DbScheduleServiceOrder}) and the manager's entry form ({@code DbRegisterVehicleEntry}).
 * Business hours and slot boundaries follow the shop's local civil time, not the server timezone.
 */
public final class SchedulingRules {

  public static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");
  private static final int OPENING_HOUR = 8;
  private static final int CLOSING_HOUR = 18;

  private final CountServiceOrdersScheduledInRangeRepository countRepository;
  private final int maxVehiclesPerHour;

  public SchedulingRules(
      CountServiceOrdersScheduledInRangeRepository countRepository, int maxVehiclesPerHour) {
    this.countRepository = countRepository;
    this.maxVehiclesPerHour = maxVehiclesPerHour;
  }

  /** Monday to Saturday, 08:00 to 18:00 shop time. */
  public void checkBusinessHours(Instant scheduledAt) throws OutsideBusinessHoursError {
    ZonedDateTime local = scheduledAt.atZone(STORE_ZONE);
    int hour = local.getHour();
    if (DayOfWeek.SUNDAY.equals(local.getDayOfWeek())
        || hour < OPENING_HOUR
        || hour >= CLOSING_HOUR) {
      throw new OutsideBusinessHoursError(scheduledAt);
    }
  }

  public void checkLeadTime(Instant scheduledAt, Instant now, Duration minLeadTime)
      throws InsufficientLeadTimeError {
    if (scheduledAt.isBefore(now.plus(minLeadTime))) {
      throw new InsufficientLeadTimeError(scheduledAt, minLeadTime);
    }
  }

  /** At most {@code maxVehiclesPerHour} orders scheduled within the same shop-local hour. */
  public void checkCapacity(Instant scheduledAt) throws SlotFullyBookedError {
    Instant slotStart = scheduledAt.atZone(STORE_ZONE).truncatedTo(ChronoUnit.HOURS).toInstant();
    Instant slotEnd = slotStart.plus(1, ChronoUnit.HOURS);
    if (countRepository.countScheduledBetween(slotStart, slotEnd) >= maxVehiclesPerHour) {
      throw new SlotFullyBookedError(scheduledAt, maxVehiclesPerHour);
    }
  }
}
