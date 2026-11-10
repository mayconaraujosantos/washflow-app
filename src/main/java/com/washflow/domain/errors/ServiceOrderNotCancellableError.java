package com.washflow.domain.errors;

public class ServiceOrderNotCancellableError extends Exception {

  public ServiceOrderNotCancellableError() {
    super("Só dá para cancelar atendimentos agendados ou aguardando no pátio");
  }
}
