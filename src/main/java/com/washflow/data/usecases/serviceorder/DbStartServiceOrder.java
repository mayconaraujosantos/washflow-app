package com.washflow.data.usecases.serviceorder;

import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.serviceorder.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.StartServiceOrder;

/**
 * Only talks to {@code data.protocols.db} ports, same as {@code DbCheckInServiceOrder} - never to
 * JDBI, Postgres, or anything else concrete.
 */
public class DbStartServiceOrder implements StartServiceOrder {

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  public DbStartServiceOrder(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.updateServiceOrderStatusRepository = updateServiceOrderStatusRepository;
  }

  @Override
  public ServiceOrder start(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError {
    ServiceOrder serviceOrder =
        loadServiceOrderByIdRepository
            .loadById(params.serviceOrderId())
            .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));

    if (serviceOrder.status() != ServiceOrderStatus.WAITING_IN_YARD) {
      throw new InvalidServiceOrderStatusError(
          serviceOrder.status(), ServiceOrderStatus.WAITING_IN_YARD);
    }

    // Compare-and-set: a concurrent request may have moved the order since it was loaded.
    return updateServiceOrderStatusRepository
        .updateStatus(
            params.serviceOrderId(),
            ServiceOrderStatus.WAITING_IN_YARD,
            ServiceOrderStatus.WASHING,
            params.washerId())
        .orElseThrow(() -> changedConcurrently(params.serviceOrderId()));
  }

  private InvalidServiceOrderStatusError changedConcurrently(java.util.UUID id) {
    ServiceOrderStatus current =
        loadServiceOrderByIdRepository
            .loadById(id)
            .map(ServiceOrder::status)
            .orElse(ServiceOrderStatus.WAITING_IN_YARD);
    return new InvalidServiceOrderStatusError(current, ServiceOrderStatus.WAITING_IN_YARD);
  }
}
