package com.washflow.data.protocols.messaging;

/** The WhatsApp provider failed or refused; the reason is for the log, not for the customer. */
public class WhatsappGatewayError extends RuntimeException {

  public WhatsappGatewayError(String message) {
    super(message);
  }

  public WhatsappGatewayError(String message, Throwable cause) {
    super(message, cause);
  }
}
