package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.AuthenticationError;

public class InvalidCredentialsError extends AuthenticationError {

  public InvalidCredentialsError() {
    super("INVALID_CREDENTIALS", "Login ou senha inválidos");
  }
}
