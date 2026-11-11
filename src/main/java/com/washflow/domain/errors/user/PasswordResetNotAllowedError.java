package com.washflow.domain.errors.user;

/**
 * Managers reset customer and washer passwords only; a manager's password is reset by the platform
 * administrator, so one manager cannot take over another's account.
 */
public class PasswordResetNotAllowedError extends RuntimeException {

  public PasswordResetNotAllowedError() {
    super("A senha de gerente é redefinida pelo administrador da plataforma");
  }
}
