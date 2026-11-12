package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.ConflictError;

public class EmailAlreadyRegisteredError extends ConflictError {

  public EmailAlreadyRegisteredError() {
    super("EMAIL_ALREADY_REGISTERED", "E-mail já cadastrado em outra conta");
  }
}
