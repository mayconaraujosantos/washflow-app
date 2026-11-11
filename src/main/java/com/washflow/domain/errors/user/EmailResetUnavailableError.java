package com.washflow.domain.errors.user;

/** No e-mail channel configured (SMTP_HOST): the code cannot be sent. */
public class EmailResetUnavailableError extends Exception {

  public EmailResetUnavailableError() {
    super("Envio de código por e-mail não está configurado");
  }
}
