package com.washflow.domain.entities;

/**
 * {@code usuarios.perfil} from CLAUDE.md - restricted to CLIENTE, LAVADOR, GERENTE. {@link
 * #dbValue()} is the exact Portuguese literal the CHECK constraint and stored rows use, same
 * convention as {@link ServiceOrderStatus}.
 */
public enum UserProfile {
  CUSTOMER("CLIENTE"),
  WASHER("LAVADOR"),
  MANAGER("GERENTE");

  private final String dbValue;

  UserProfile(String dbValue) {
    this.dbValue = dbValue;
  }

  public String dbValue() {
    return dbValue;
  }

  /** Reverses {@link #dbValue()} - used by repositories mapping a stored {@code perfil} column. */
  public static UserProfile fromDbValue(String dbValue) {
    for (UserProfile profile : values()) {
      if (profile.dbValue.equals(dbValue)) {
        return profile;
      }
    }
    throw new IllegalArgumentException("Unknown user profile: " + dbValue);
  }
}
