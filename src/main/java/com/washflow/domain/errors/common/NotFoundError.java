package com.washflow.domain.errors.common;

/** The resource the request points to does not exist (or is not visible to the caller). */
public abstract non-sealed class NotFoundError extends DomainError {

  protected NotFoundError(String code, String message) {
    super(code, message);
  }
}
