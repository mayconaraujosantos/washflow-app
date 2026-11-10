package com.washflow.data.protocols.messaging;

/** Delivers a plain-text e-mail, e.g. a password-reset code. */
public interface EmailSender {

  /**
   * @throws EmailDeliveryException when the message could not be handed to the mail server
   */
  void send(String to, String subject, String body);

  /** False when no channel is configured: the app hides the e-mail options. */
  boolean available();

  class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
