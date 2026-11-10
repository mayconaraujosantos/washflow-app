package com.washflow.domain.errors.user;

public class InvalidCredentialsError extends Exception {

  public InvalidCredentialsError() {
    super("Login ou senha inválidos");
  }
}
