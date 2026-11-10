package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderRefund;
import com.washflow.domain.entities.shared.PaymentMethod;
import java.util.Optional;
import java.util.UUID;

/** Port for refunding a finalized order. Empty when the order is not (or no longer) finalized. */
public interface RefundServiceOrderRepository {

  Optional<ServiceOrderRefund> refund(
      UUID orderId,
      UUID managerId,
      String reason,
      String description,
      PaymentMethod refundMethod,
      boolean reverseCommission);
}
