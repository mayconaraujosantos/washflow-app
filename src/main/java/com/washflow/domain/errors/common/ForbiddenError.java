package com.washflow.domain.errors.common;

/**
 * Generic "not authorized for this action" error - e.g. a non-manager calling a manager-only use
 * case.
 */
public class ForbiddenError extends AccessDeniedError {

  public ForbiddenError(String message) {
    super("FORBIDDEN", message);
  }
}
