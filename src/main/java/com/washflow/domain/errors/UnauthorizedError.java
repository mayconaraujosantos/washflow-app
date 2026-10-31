package com.washflow.domain.errors;

/** Generic "not authenticated" error - e.g. missing token, expired token, or inactive account. */
public class UnauthorizedError extends Exception {

  public UnauthorizedError(String message) {
    super(message);
  }
}
