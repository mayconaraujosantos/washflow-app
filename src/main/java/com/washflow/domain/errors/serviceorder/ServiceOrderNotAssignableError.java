package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

/** The washer can only be chosen before the wash starts (scheduled or waiting in the yard). */
public class ServiceOrderNotAssignableError extends ConflictError {

  public ServiceOrderNotAssignableError() {
    super(
        "SERVICE_ORDER_NOT_ASSIGNABLE",
        "A lavagem deste carro já começou; não dá mais para trocar o lavador.");
  }
}
