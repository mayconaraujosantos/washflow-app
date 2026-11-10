package com.washflow.data.usecases.schedule;

import com.washflow.data.protocols.db.schedule.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.serviceorder.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.shared.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.vehicle.LoadVehicleByIdRepository;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.cash.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import com.washflow.domain.usecases.schedule.ScheduleServiceOrder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete. Swapping the persistence library later (Hibernate, jOOQ, whatever) means writing new
 * {@code infra.db.*} implementations of those same ports; this class doesn't change.
 *
 * <p>Enforces the MVP rules from {@code docs/feature.spec.md} 1.4 through {@link SchedulingRules}:
 * business hours, minimum lead time, and per-hour capacity. {@code clock} is injected (not {@link
 * Instant#now()} directly) so "now" is deterministic in tests - see {@code
 * DbScheduleServiceOrderTest}.
 */
public class DbScheduleServiceOrder implements ScheduleServiceOrder {

  private static final Duration MIN_LEAD_TIME = Duration.ofMinutes(30);

  private final LoadVehicleByIdRepository loadVehicleByIdRepository;
  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final CreateServiceOrderRepository createServiceOrderRepository;
  private final Clock clock;
  private final SchedulingRules rules;

  public DbScheduleServiceOrder(
      LoadVehicleByIdRepository loadVehicleByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      CountServiceOrdersScheduledInRangeRepository countServiceOrdersScheduledInRangeRepository,
      CreateServiceOrderRepository createServiceOrderRepository,
      Clock clock,
      int maxVehiclesPerHour) {
    this.loadVehicleByIdRepository = loadVehicleByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.createServiceOrderRepository = createServiceOrderRepository;
    this.clock = clock;
    this.rules =
        new SchedulingRules(countServiceOrdersScheduledInRangeRepository, maxVehiclesPerHour);
  }

  /** Hours and capacity from the establishment settings (see SchedulingRules.Hours). */
  public DbScheduleServiceOrder(
      LoadVehicleByIdRepository loadVehicleByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      CountServiceOrdersScheduledInRangeRepository countServiceOrdersScheduledInRangeRepository,
      CreateServiceOrderRepository createServiceOrderRepository,
      Clock clock,
      java.util.function.Supplier<SchedulingRules.Hours> hours) {
    this.loadVehicleByIdRepository = loadVehicleByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.createServiceOrderRepository = createServiceOrderRepository;
    this.clock = clock;
    this.rules = new SchedulingRules(countServiceOrdersScheduledInRangeRepository, hours);
  }

  @Override
  public ServiceOrder schedule(Params params)
      throws VehicleNotFoundError,
          ServicePriceNotFoundError,
          OutsideBusinessHoursError,
          InsufficientLeadTimeError,
          SlotFullyBookedError {
    Instant now = Instant.now(clock);
    rules.checkBusinessHours(params.scheduledAt());
    rules.checkLeadTime(params.scheduledAt(), now, MIN_LEAD_TIME);

    loadVehicleByIdRepository
        .loadById(params.vehicleId())
        .orElseThrow(() -> new VehicleNotFoundError(params.vehicleId()));

    loadServicePriceByIdRepository
        .loadById(params.servicePriceId())
        .orElseThrow(() -> new ServicePriceNotFoundError(params.servicePriceId()));

    rules.checkCapacity(params.scheduledAt());

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

    return rules.book(createServiceOrderRepository, newServiceOrder);
  }
}
