package com.washflow.domain.errors.auth;

import com.washflow.domain.errors.common.TooManyRequestsError;

public class OtpTooSoonError extends TooManyRequestsError {

  public OtpTooSoonError() {
    super("OTP_TOO_SOON", "Aguarde um minuto para pedir outro código");
  }
}
