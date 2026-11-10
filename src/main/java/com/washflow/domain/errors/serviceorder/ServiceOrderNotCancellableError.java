package com.washflow.domain.errors.serviceorder;

public class ServiceOrderNotCancellableError extends Exception {

  public ServiceOrderNotCancellableError() {
    super("Só dá para cancelar atendimentos agendados ou aguardando no pátio");
  }
}
