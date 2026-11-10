package com.washflow.domain.errors.auth;

public class OtpUnavailableError extends Exception {

  public OtpUnavailableError() {
    super("Envio de código por WhatsApp não está configurado");
  }
}
