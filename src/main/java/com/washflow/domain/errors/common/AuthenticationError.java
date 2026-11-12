package com.washflow.domain.errors.common;

/** The caller could not be identified: no session, an expired one, or wrong credentials. */
public abstract non-sealed class AuthenticationError extends DomainError {

  protected AuthenticationError(String code, String message) {
    super(code, message);
  }
}
