package com.washflow.domain.errors.auth;

public class OtpUnavailableError extends Exception {

  public OtpUnavailableError() {
    this("Envio de código por WhatsApp não está configurado");
  }

  public OtpUnavailableError(String message) {
    super(message);
  }
}
