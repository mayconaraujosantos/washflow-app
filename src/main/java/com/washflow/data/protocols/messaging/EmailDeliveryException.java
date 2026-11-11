package com.washflow.data.protocols.messaging;

/** The e-mail could not be handed to the mail server (see {@link EmailSender#send}). */
public class EmailDeliveryException extends RuntimeException {

  public EmailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
