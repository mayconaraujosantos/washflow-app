package com.washflow.domain.errors.sale;

import com.washflow.domain.errors.common.ConflictError;

/** A budget, or more than what came in through the chosen payment method. */
public class SaleRefundNotAllowedError extends ConflictError {

  public SaleRefundNotAllowedError(String message) {
    super("SALE_REFUND_NOT_ALLOWED", message);
  }
}
