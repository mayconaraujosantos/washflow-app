package com.washflow.data.usecases.finalizeserviceorder;

import com.washflow.data.protocols.db.CreateWasherCommissionRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.entities.WasherCommission;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.usecases.FinalizeServiceOrder;
import java.time.Instant;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports, same as {@code DbCompleteServiceOrder} - never to
 * JDBI, Postgres, or anything else concrete.
 */
public class DbFinalizeServiceOrder implements FinalizeServiceOrder {

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;
  private final CreateWasherCommissionRepository createWasherCommissionRepository;

  public DbFinalizeServiceOrder(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository,
      CreateWasherCommissionRepository createWasherCommissionRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.updateServiceOrderStatusRepository = updateServiceOrderStatusRepository;
    this.createWasherCommissionRepository = createWasherCommissionRepository;
  }

  @Override
  public ServiceOrder finalize(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError, ServicePriceNotFoundError {
    ServiceOrder serviceOrder =
        loadServiceOrderByIdRepository
            .loadById(params.serviceOrderId())
            .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));

    if (serviceOrder.status() != ServiceOrderStatus.READY) {
      throw new InvalidServiceOrderStatusError(serviceOrder.status(), ServiceOrderStatus.READY);
    }

    ServicePrice servicePrice =
        loadServicePriceByIdRepository
            .loadById(serviceOrder.servicePriceId())
            .orElseThrow(() -> new ServicePriceNotFoundError(serviceOrder.servicePriceId()));

    ServiceOrder finalizedServiceOrder =
        updateServiceOrderStatusRepository.updateStatus(
            params.serviceOrderId(), ServiceOrderStatus.DONE, serviceOrder.washerId());

    createWasherCommissionRepository.create(
        new WasherCommission(
            UUID.randomUUID(),
            serviceOrder.id(),
            serviceOrder.washerId(),
            servicePrice.washerCommission(),
            Instant.now()));

    return finalizedServiceOrder;
  }
}
