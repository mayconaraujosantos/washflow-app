package com.washflow.data.usecases.shared;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.usecases.schedule.DbScheduleServiceOrder;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * The scheduling rules from {@code docs/feature.spec.md} 1.4, shared by customer self-scheduling
 * ({@link DbScheduleServiceOrder}) and the manager's entry form ({@code DbRegisterVehicleEntry}).
 * Business hours and slot boundaries follow the shop's local civil time, not the server timezone.
 */
public final class SchedulingRules {

  public static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");

  /** Opening hours and capacity; read on every check so settings changes apply at once. */
  public record Hours(
      int openingHour, int closingHour, boolean openOnSunday, int maxVehiclesPerHour) {}

  private final CountServiceOrdersScheduledInRangeRepository countRepository;
  private final Supplier<Hours> hours;

  /** Fixed Monday-Saturday 08:00-18:00 schedule (tests and the pre-settings default). */
  public SchedulingRules(
      CountServiceOrdersScheduledInRangeRepository countRepository, int maxVehiclesPerHour) {
    this(countRepository, () -> new Hours(8, 18, false, maxVehiclesPerHour));
  }

  public SchedulingRules(
      CountServiceOrdersScheduledInRangeRepository countRepository, Supplier<Hours> hours) {
    this.countRepository = countRepository;
    this.hours = hours;
  }

  /** Inside the establishment's opening hours, shop time; Sundays only when it opens. */
  public void checkBusinessHours(Instant scheduledAt) throws OutsideBusinessHoursError {
    Hours current = hours.get();
    ZonedDateTime local = scheduledAt.atZone(STORE_ZONE);
    int hour = local.getHour();
    if ((DayOfWeek.SUNDAY.equals(local.getDayOfWeek()) && !current.openOnSunday())
        || hour < current.openingHour()
        || hour >= current.closingHour()) {
      throw new OutsideBusinessHoursError(
          scheduledAt, current.openingHour(), current.closingHour(), current.openOnSunday());
    }
  }

  public void checkLeadTime(Instant scheduledAt, Instant now, Duration minLeadTime)
      throws InsufficientLeadTimeError {
    if (scheduledAt.isBefore(now.plus(minLeadTime))) {
      throw new InsufficientLeadTimeError(scheduledAt, minLeadTime);
    }
  }

  /**
   * At most {@code maxVehiclesPerHour} orders scheduled within the same shop-local hour. A quick
   * early check (before creating the vehicle); the booking itself goes through {@link #book}.
   */
  public void checkCapacity(Instant scheduledAt) throws SlotFullyBookedError {
    int maxVehiclesPerHour = hours.get().maxVehiclesPerHour();
    Instant slotStart = slotStart(scheduledAt);
    Instant slotEnd = slotStart.plus(1, ChronoUnit.HOURS);
    if (countRepository.countScheduledBetween(slotStart, slotEnd) >= maxVehiclesPerHour) {
      throw new SlotFullyBookedError(scheduledAt, maxVehiclesPerHour);
    }
  }

  /**
   * Creates the scheduled order only if its hour still has room, counting and inserting atomically:
   * simultaneous bookings can't overfill the slot.
   */
  public ServiceOrder book(CreateServiceOrderRepository repository, ServiceOrder serviceOrder)
      throws SlotFullyBookedError {
    int maxVehiclesPerHour = hours.get().maxVehiclesPerHour();
    Instant slotStart = slotStart(serviceOrder.scheduledAt());
    return repository
        .createWithinCapacity(
            serviceOrder, slotStart, slotStart.plus(1, ChronoUnit.HOURS), maxVehiclesPerHour)
        .orElseThrow(
            () -> new SlotFullyBookedError(serviceOrder.scheduledAt(), maxVehiclesPerHour));
  }

  private static Instant slotStart(Instant scheduledAt) {
    return scheduledAt.atZone(STORE_ZONE).truncatedTo(ChronoUnit.HOURS).toInstant();
  }
}
