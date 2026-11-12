package com.washflow.domain.errors.common;

/** Generic "not authenticated" error - e.g. missing token, expired token, or inactive account. */
public class UnauthorizedError extends AuthenticationError {

  public UnauthorizedError(String message) {
    super("UNAUTHORIZED", message);
  }
}
