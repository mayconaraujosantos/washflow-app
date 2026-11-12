package com.washflow.domain.errors.auth;

import com.washflow.domain.errors.common.AuthenticationError;

public class InvalidOtpError extends AuthenticationError {

  public InvalidOtpError() {
    super("INVALID_OTP", "Código inválido ou expirado");
  }
}
