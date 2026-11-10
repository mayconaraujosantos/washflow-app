package com.washflow.domain.errors.serviceorder;

public class ServiceOrderNotRefundableError extends Exception {

  public ServiceOrderNotRefundableError() {
    super("Só dá para estornar atendimentos finalizados, uma única vez");
  }
}
