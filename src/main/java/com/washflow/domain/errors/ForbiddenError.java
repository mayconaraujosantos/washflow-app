package com.washflow.domain.errors;

/**
 * Generic "not authorized for this action" error - e.g. a non-manager calling a manager-only use
 * case.
 */
public class ForbiddenError extends Exception {

  public ForbiddenError(String message) {
    super(message);
  }
}
