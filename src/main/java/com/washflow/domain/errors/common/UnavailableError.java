package com.washflow.domain.errors.common;

/**
 * A feature the request depends on is not configured or its provider failed (e-mail, WhatsApp,
 * Google); the request may work later.
 */
public abstract non-sealed class UnavailableError extends DomainError {

  protected UnavailableError(String code, String message) {
    super(code, message);
  }
}
