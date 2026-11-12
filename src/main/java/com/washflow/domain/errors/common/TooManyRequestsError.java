package com.washflow.domain.errors.common;

/** The caller is asking too often: too many failed attempts or a new code before the cooldown. */
public abstract non-sealed class TooManyRequestsError extends DomainError {

  protected TooManyRequestsError(String code, String message) {
    super(code, message);
  }
}
