package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.AccessDeniedError;

/**
 * Managers reset customer and washer passwords only; a manager's password is reset by the platform
 * administrator, so one manager cannot take over another's account.
 */
public class PasswordResetNotAllowedError extends AccessDeniedError {

  public PasswordResetNotAllowedError() {
    super(
        "PASSWORD_RESET_NOT_ALLOWED",
        "A senha de gerente é redefinida pelo administrador da plataforma");
  }
}
