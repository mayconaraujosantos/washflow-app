package com.washflow.data.usecases.finalizeserviceorder;

import com.washflow.data.protocols.db.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
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
  private final FinalizeServiceOrderRepository finalizeServiceOrderRepository;

  public DbFinalizeServiceOrder(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      FinalizeServiceOrderRepository finalizeServiceOrderRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.finalizeServiceOrderRepository = finalizeServiceOrderRepository;
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

    Instant receivedAt = Instant.now();
    return finalizeServiceOrderRepository.finalizeSale(
        serviceOrder,
        new ServicePayment(
            UUID.randomUUID(),
            serviceOrder.id(),
            params.paymentMethod(),
            servicePrice.price(),
            receivedAt),
        new WasherCommission(
            UUID.randomUUID(),
            serviceOrder.id(),
            serviceOrder.washerId(),
            servicePrice.washerCommission(),
            receivedAt));
  }
}
