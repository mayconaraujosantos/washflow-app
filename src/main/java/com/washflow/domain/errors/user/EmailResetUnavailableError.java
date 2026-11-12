package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.UnavailableError;

/** No e-mail channel configured (SMTP_HOST): the code cannot be sent. */
public class EmailResetUnavailableError extends UnavailableError {

  public EmailResetUnavailableError() {
    super("EMAIL_RESET_UNAVAILABLE", "Envio de código por e-mail não está configurado");
  }
}
