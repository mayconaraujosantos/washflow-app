package com.washflow.domain.errors.auth;

public class InvalidOtpError extends Exception {

  public InvalidOtpError() {
    super("Código inválido ou expirado");
  }
}
