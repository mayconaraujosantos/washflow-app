package com.washflow.domain.errors;

public class EmailAlreadyRegisteredError extends Exception {

  public EmailAlreadyRegisteredError() {
    super("E-mail já cadastrado em outra conta");
  }
}
