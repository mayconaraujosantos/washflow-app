package com.washflow.domain.usecases;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrderRefund;
import com.washflow.domain.errors.ServiceOrderNotRefundableError;
import java.util.UUID;

/**
 * Refunds the full payment of a finalized order. {@code refundMethod} null means the method the
 * customer paid with; the washer's commission is only reversed when {@code reverseCommission}.
 * Implemented by {@code DbRefundServiceOrder} in the data layer.
 */
public interface RefundServiceOrder {

  ServiceOrderRefund refund(Params params) throws ServiceOrderNotRefundableError;

  record Params(
      UUID serviceOrderId,
      UUID managerId,
      String reason,
      String description,
      PaymentMethod refundMethod,
      boolean reverseCommission) {}
}
