package com.washflow.domain.errors.common;

/** The request itself is invalid: a missing or malformed field, or a value out of range. */
public abstract non-sealed class ValidationError extends DomainError {

  protected ValidationError(String code, String message) {
    super(code, message);
  }
}
