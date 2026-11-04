package com.washflow.data.protocols.db;

/**
 * A write hit a unique rule of the database (the same row written twice, usually by two requests
 * racing each other). Repositories throw it instead of the driver's exception so use cases can turn
 * the race into the right answer - a login, a 409 - instead of a 500.
 */
public class DuplicateKeyError extends RuntimeException {

  private final String constraint;

  public DuplicateKeyError(String constraint, Throwable cause) {
    super("Duplicate key" + (constraint == null ? "" : " (" + constraint + ")"), cause);
    this.constraint = constraint;
  }

  /** Name of the violated constraint/index, when the database reports it. */
  public String constraint() {
    return constraint;
  }

  public boolean is(String name) {
    return name.equals(constraint);
  }
}
