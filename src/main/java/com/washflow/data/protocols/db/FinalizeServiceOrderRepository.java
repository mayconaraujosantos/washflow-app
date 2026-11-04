package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.WasherCommission;
import java.util.Optional;

public interface FinalizeServiceOrderRepository {

  /**
   * Finalizes a {@code PRONTO} order, its payment and the washer commission in one transaction.
   * Empty (and nothing written) when the order is no longer {@code PRONTO}: it was finalized by a
   * concurrent request.
   */
  Optional<ServiceOrder> finalizeSale(
      ServiceOrder serviceOrder, ServicePayment payment, WasherCommission commission);
}
