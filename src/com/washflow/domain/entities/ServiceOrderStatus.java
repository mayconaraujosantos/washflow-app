package com.washflow.domain.entities;

/**
 * The vehicle status flow from CLAUDE.md - linear, one direction. {@link #dbValue()} is the exact
 * Portuguese literal CLAUDE.md's schema/queries use ({@code AGENDADO}, {@code AGUARDANDO_PATIO}...)
 * - that's the business's domain language and the actual stored/queried value, kept as-is even
 * though the Java-side constant names follow the English code convention.
 */
public enum ServiceOrderStatus {
  SCHEDULED("AGENDADO"),
  WAITING_IN_YARD("AGUARDANDO_PATIO"),
  WASHING("EM_LAVAGEM"),
  READY("PRONTO"),
  DONE("FINALIZADO");

  private final String dbValue;

  ServiceOrderStatus(String dbValue) {
    this.dbValue = dbValue;
  }

  public String dbValue() {
    return dbValue;
  }
}
