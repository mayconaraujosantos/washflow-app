package com.washflow.data.usecases.serviceorder;

import com.washflow.data.protocols.db.serviceorder.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.shared.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.pricing.ServicePrice;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.serviceorder.ServicePayment;
import com.washflow.domain.entities.serviceorder.WasherCommission;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.FinalizeServiceOrder;
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
    return finalizeServiceOrderRepository
        .finalizeSale(
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
                receivedAt))
        // Finalized by a concurrent request (double click): one payment only, this one is a 409.
        .orElseThrow(
            () ->
                new InvalidServiceOrderStatusError(
                    ServiceOrderStatus.DONE, ServiceOrderStatus.READY));
  }
}
