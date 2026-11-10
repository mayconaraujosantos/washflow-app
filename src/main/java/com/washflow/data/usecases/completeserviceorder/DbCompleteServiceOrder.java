package com.washflow.data.usecases.completeserviceorder;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.CompleteServiceOrder;

/**
 * Only talks to {@code data.protocols.db} ports, same as {@code DbStartServiceOrder} - never to
 * JDBI, Postgres, or anything else concrete.
 */
public class DbCompleteServiceOrder implements CompleteServiceOrder {

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  public DbCompleteServiceOrder(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.updateServiceOrderStatusRepository = updateServiceOrderStatusRepository;
  }

  @Override
  public ServiceOrder complete(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError {
    ServiceOrder serviceOrder =
        loadServiceOrderByIdRepository
            .loadById(params.serviceOrderId())
            .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));

    if (serviceOrder.status() != ServiceOrderStatus.WASHING) {
      throw new InvalidServiceOrderStatusError(serviceOrder.status(), ServiceOrderStatus.WASHING);
    }

    // Compare-and-set: a concurrent request may have moved the order since it was loaded.
    return updateServiceOrderStatusRepository
        .updateStatus(
            params.serviceOrderId(),
            ServiceOrderStatus.WASHING,
            ServiceOrderStatus.READY,
            serviceOrder.washerId())
        .orElseThrow(() -> changedConcurrently(params.serviceOrderId()));
  }

  private InvalidServiceOrderStatusError changedConcurrently(java.util.UUID id) {
    ServiceOrderStatus current =
        loadServiceOrderByIdRepository
            .loadById(id)
            .map(ServiceOrder::status)
            .orElse(ServiceOrderStatus.WASHING);
    return new InvalidServiceOrderStatusError(current, ServiceOrderStatus.WASHING);
  }
}
