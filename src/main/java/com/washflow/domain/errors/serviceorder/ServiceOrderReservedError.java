package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

/** The manager reserved this car for another washer. */
public class ServiceOrderReservedError extends ConflictError {

  public ServiceOrderReservedError() {
    super("SERVICE_ORDER_RESERVED", "Este carro foi reservado para outro lavador pelo gerente.");
  }
}
