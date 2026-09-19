package com.washflow.data.usecases.scheduleserviceorder;

import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import java.time.Instant;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete. Swapping the persistence library later (Hibernate, jOOQ, whatever) means writing new
 * {@code infra.db.*} implementations of those same ports; this class doesn't change.
 */
public class DbScheduleServiceOrder implements ScheduleServiceOrder {

  private final LoadVehicleByIdRepository loadVehicleByIdRepository;
  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final CreateServiceOrderRepository createServiceOrderRepository;

  public DbScheduleServiceOrder(
      LoadVehicleByIdRepository loadVehicleByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      CreateServiceOrderRepository createServiceOrderRepository) {
    this.loadVehicleByIdRepository = loadVehicleByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.createServiceOrderRepository = createServiceOrderRepository;
  }

  @Override
  public ServiceOrder schedule(Params params)
      throws VehicleNotFoundError, ServicePriceNotFoundError {
    loadVehicleByIdRepository
        .loadById(params.vehicleId())
        .orElseThrow(() -> new VehicleNotFoundError(params.vehicleId()));

    loadServicePriceByIdRepository
        .loadById(params.servicePriceId())
        .orElseThrow(() -> new ServicePriceNotFoundError(params.servicePriceId()));

    Instant now = Instant.now();
    ServiceOrder newServiceOrder =
        new ServiceOrder(
            UUID.randomUUID(),
            params.vehicleId(),
            params.servicePriceId(),
            ServiceOrderStatus.SCHEDULED,
            params.scheduledAt(),
            now,
            now);

    return createServiceOrderRepository.create(newServiceOrder);
  }
}
