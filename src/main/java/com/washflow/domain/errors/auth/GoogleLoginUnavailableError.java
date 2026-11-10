package com.washflow.domain.errors.auth;

public class GoogleLoginUnavailableError extends Exception {

  public GoogleLoginUnavailableError() {
    super("Login com Google não está configurado");
  }
}
