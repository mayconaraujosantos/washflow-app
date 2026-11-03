package com.washflow.domain.errors;

public class InvalidOtpError extends Exception {

  public InvalidOtpError() {
    super("Código inválido ou expirado");
  }
}
