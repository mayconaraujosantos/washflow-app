package com.washflow.domain.errors;

public class GoogleLoginUnavailableError extends Exception {

  public GoogleLoginUnavailableError() {
    super("Login com Google não está configurado");
  }
}
