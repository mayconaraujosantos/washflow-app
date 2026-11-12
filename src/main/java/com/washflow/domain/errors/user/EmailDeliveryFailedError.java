package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.UnavailableError;

/** The mail server refused or did not answer: try again in a moment. */
public class EmailDeliveryFailedError extends UnavailableError {

  public EmailDeliveryFailedError() {
    super(
        "EMAIL_DELIVERY_FAILED",
        "Não foi possível enviar o e-mail agora. Tente de novo em instantes");
  }
}
