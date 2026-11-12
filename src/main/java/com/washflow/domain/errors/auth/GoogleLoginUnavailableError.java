package com.washflow.domain.errors.auth;

import com.washflow.domain.errors.common.UnavailableError;

public class GoogleLoginUnavailableError extends UnavailableError {

  public GoogleLoginUnavailableError() {
    super("GOOGLE_LOGIN_UNAVAILABLE", "Login com Google não está configurado");
  }
}
