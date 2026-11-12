package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

public class ServiceOrderNotCancellableError extends ConflictError {

  public ServiceOrderNotCancellableError() {
    super(
        "SERVICE_ORDER_NOT_CANCELLABLE",
        "Só dá para cancelar atendimentos agendados ou aguardando no pátio");
  }
}
