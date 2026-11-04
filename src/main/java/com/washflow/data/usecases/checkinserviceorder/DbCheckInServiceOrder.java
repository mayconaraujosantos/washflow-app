package com.washflow.data.usecases.checkinserviceorder;

import com.washflow.data.protocols.db.DuplicateKeyError;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.VehicleAlreadyInYardError;
import com.washflow.domain.usecases.CheckInServiceOrder;

/**
 * Only talks to {@code data.protocols.db} ports, same as {@code DbScheduleServiceOrder} - never to
 * JDBI, Postgres, or anything else concrete.
 */
public class DbCheckInServiceOrder implements CheckInServiceOrder {

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  public DbCheckInServiceOrder(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.updateServiceOrderStatusRepository = updateServiceOrderStatusRepository;
  }

  @Override
  public ServiceOrder checkIn(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError, VehicleAlreadyInYardError {
    ServiceOrder serviceOrder =
        loadServiceOrderByIdRepository
            .loadById(params.serviceOrderId())
            .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));

    if (serviceOrder.status() != ServiceOrderStatus.SCHEDULED) {
      throw new InvalidServiceOrderStatusError(serviceOrder.status(), ServiceOrderStatus.SCHEDULED);
    }

    try {
      // Compare-and-set: a concurrent request may have moved the order since it was loaded.
      return updateServiceOrderStatusRepository
          .updateStatus(
              params.serviceOrderId(),
              ServiceOrderStatus.SCHEDULED,
              ServiceOrderStatus.WAITING_IN_YARD,
              null)
          .orElseThrow(() -> changedConcurrently(params.serviceOrderId()));
    } catch (DuplicateKeyError e) {
      // The same car is already in the yard with another open order.
      throw new VehicleAlreadyInYardError(
          serviceOrder.details() == null ? null : serviceOrder.details().vehiclePlate());
    }
  }

  private InvalidServiceOrderStatusError changedConcurrently(java.util.UUID id) {
    ServiceOrderStatus current =
        loadServiceOrderByIdRepository
            .loadById(id)
            .map(ServiceOrder::status)
            .orElse(ServiceOrderStatus.SCHEDULED);
    return new InvalidServiceOrderStatusError(current, ServiceOrderStatus.SCHEDULED);
  }
}
