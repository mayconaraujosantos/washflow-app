package com.washflow.domain.errors.establishment;

public class InvalidSetupCodeError extends Exception {

  public InvalidSetupCodeError() {
    super("Código de configuração inválido");
  }
}
