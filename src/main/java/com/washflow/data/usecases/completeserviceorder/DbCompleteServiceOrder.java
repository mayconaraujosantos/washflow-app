package com.washflow.data.usecases.completeserviceorder;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.CompleteServiceOrder;

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

    return updateServiceOrderStatusRepository.updateStatus(
        params.serviceOrderId(), ServiceOrderStatus.READY, serviceOrder.washerId());
  }
}
