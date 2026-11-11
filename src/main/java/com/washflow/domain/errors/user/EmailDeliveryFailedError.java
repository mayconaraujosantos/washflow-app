package com.washflow.domain.errors.user;

/** The mail server refused or did not answer: try again in a moment. */
public class EmailDeliveryFailedError extends Exception {

  public EmailDeliveryFailedError() {
    super("Não foi possível enviar o e-mail agora. Tente de novo em instantes");
  }
}
