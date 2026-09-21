package com.washflow.data.usecases.scheduleserviceorder;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete. Swapping the persistence library later (Hibernate, jOOQ, whatever) means writing new
 * {@code infra.db.*} implementations of those same ports; this class doesn't change.
 *
 * <p>Enforces the MVP rules from {@code docs/feature.spec.md} 1.4: business hours, minimum lead
 * time, and per-hour capacity. {@code clock} is injected (not {@link Instant#now()} directly) so
 * "now" is deterministic in tests - see {@code DbScheduleServiceOrderTest}.
 */
public class DbScheduleServiceOrder implements ScheduleServiceOrder {

  // The shop's local timezone - business hours and slot boundaries are evaluated against this,
  // not UTC. Brazil suspended DST nationally in 2019, so this stays at a fixed UTC-3 offset.
  private static final ZoneId STORE_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final int OPENING_HOUR = 8;
  private static final int CLOSING_HOUR = 18;
  private static final Duration MIN_LEAD_TIME = Duration.ofMinutes(30);

  private final LoadVehicleByIdRepository loadVehicleByIdRepository;
  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final CountServiceOrdersScheduledInRangeRepository
      countServiceOrdersScheduledInRangeRepository;
  private final CreateServiceOrderRepository createServiceOrderRepository;
  private final Clock clock;
  private final int maxVehiclesPerHour;

  public DbScheduleServiceOrder(
      LoadVehicleByIdRepository loadVehicleByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      CountServiceOrdersScheduledInRangeRepository countServiceOrdersScheduledInRangeRepository,
      CreateServiceOrderRepository createServiceOrderRepository,
      Clock clock,
      int maxVehiclesPerHour) {
    this.loadVehicleByIdRepository = loadVehicleByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.countServiceOrdersScheduledInRangeRepository =
        countServiceOrdersScheduledInRangeRepository;
    this.createServiceOrderRepository = createServiceOrderRepository;
    this.clock = clock;
    this.maxVehiclesPerHour = maxVehiclesPerHour;
  }

  @Override
  public ServiceOrder schedule(Params params)
      throws VehicleNotFoundError,
          ServicePriceNotFoundError,
          OutsideBusinessHoursError,
          InsufficientLeadTimeError,
          SlotFullyBookedError {
    ZonedDateTime scheduledLocal = params.scheduledAt().atZone(STORE_ZONE);
    checkBusinessHours(params.scheduledAt(), scheduledLocal);
    checkLeadTime(params.scheduledAt());

    loadVehicleByIdRepository
        .loadById(params.vehicleId())
        .orElseThrow(() -> new VehicleNotFoundError(params.vehicleId()));

    loadServicePriceByIdRepository
        .loadById(params.servicePriceId())
        .orElseThrow(() -> new ServicePriceNotFoundError(params.servicePriceId()));

    checkCapacity(params.scheduledAt(), scheduledLocal);

    Instant now = Instant.now(clock);
    ServiceOrder newServiceOrder =
        new ServiceOrder(
            UUID.randomUUID(),
            params.vehicleId(),
            params.servicePriceId(),
            null,
            ServiceOrderStatus.SCHEDULED,
            params.scheduledAt(),
            now,
            now);

    return createServiceOrderRepository.create(newServiceOrder);
  }

  private void checkBusinessHours(Instant scheduledAt, ZonedDateTime scheduledLocal)
      throws OutsideBusinessHoursError {
    int hour = scheduledLocal.getHour();
    if (DayOfWeek.SUNDAY.equals(scheduledLocal.getDayOfWeek())
        || hour < OPENING_HOUR
        || hour >= CLOSING_HOUR) {
      throw new OutsideBusinessHoursError(scheduledAt);
    }
  }

  private void checkLeadTime(Instant scheduledAt) throws InsufficientLeadTimeError {
    if (scheduledAt.isBefore(Instant.now(clock).plus(MIN_LEAD_TIME))) {
      throw new InsufficientLeadTimeError(scheduledAt, MIN_LEAD_TIME);
    }
  }

  private void checkCapacity(Instant scheduledAt, ZonedDateTime scheduledLocal)
      throws SlotFullyBookedError {
    Instant slotStart = scheduledLocal.truncatedTo(ChronoUnit.HOURS).toInstant();
    Instant slotEnd = slotStart.plus(1, ChronoUnit.HOURS);

    int bookedCount =
        countServiceOrdersScheduledInRangeRepository.countScheduledBetween(slotStart, slotEnd);
    if (bookedCount >= maxVehiclesPerHour) {
      throw new SlotFullyBookedError(scheduledAt, maxVehiclesPerHour);
    }
  }
}
