package com.washflow.domain.errors.auth;

import com.washflow.domain.errors.common.UnavailableError;

public class OtpUnavailableError extends UnavailableError {

  public OtpUnavailableError() {
    this("Envio de código por WhatsApp não está configurado");
  }

  public OtpUnavailableError(String message) {
    super("OTP_UNAVAILABLE", message);
  }
}
