package com.washflow.domain.errors.common;

/**
 * The request is valid but clashes with the current state, e.g. a status transition that already
 * happened.
 */
public abstract non-sealed class ConflictError extends DomainError {

  protected ConflictError(String code, String message) {
    super(code, message);
  }
}
