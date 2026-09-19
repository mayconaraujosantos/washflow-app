package com.washflow.presentation.errors;

/** Wraps an unexpected failure for logging; clients only ever see a generic 500 message. */
public class ServerError extends RuntimeException {

  public ServerError(Throwable cause) {
    super("Internal server error", cause);
  }
}
