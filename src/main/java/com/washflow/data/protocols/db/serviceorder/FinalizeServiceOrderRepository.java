package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServicePayment;
import com.washflow.domain.entities.serviceorder.WasherCommission;
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
