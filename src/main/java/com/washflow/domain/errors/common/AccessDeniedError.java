package com.washflow.domain.errors.common;

/** The caller is known but may not perform this action. */
public abstract non-sealed class AccessDeniedError extends DomainError {

  protected AccessDeniedError(String code, String message) {
    super(code, message);
  }
}
