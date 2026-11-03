package com.washflow.domain.errors;

public class InvalidSetupCodeError extends Exception {

  public InvalidSetupCodeError() {
    super("Código de configuração inválido");
  }
}
