package com.washflow.domain.errors;

public class InvalidCredentialsError extends Exception {

  public InvalidCredentialsError() {
    super("Telefone ou senha inválidos");
  }
}
