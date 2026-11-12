package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

public class ServiceOrderNotRefundableError extends ConflictError {

  public ServiceOrderNotRefundableError() {
    super(
        "SERVICE_ORDER_NOT_REFUNDABLE",
        "Só dá para estornar atendimentos finalizados, uma única vez");
  }
}
