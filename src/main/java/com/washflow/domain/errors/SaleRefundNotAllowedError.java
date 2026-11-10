package com.washflow.domain.errors;

/** A budget, or more than what came in through the chosen payment method. */
public class SaleRefundNotAllowedError extends Exception {

  public SaleRefundNotAllowedError(String message) {
    super(message);
  }
}
