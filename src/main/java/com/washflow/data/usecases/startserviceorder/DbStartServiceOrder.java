package com.washflow.data.usecases.startserviceorder;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.StartServiceOrder;

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

    return updateServiceOrderStatusRepository.updateStatus(
        params.serviceOrderId(), ServiceOrderStatus.WASHING, params.washerId());
  }
}
