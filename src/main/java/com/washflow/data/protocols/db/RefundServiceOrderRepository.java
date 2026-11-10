package com.washflow.data.protocols.db;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrderRefund;
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
