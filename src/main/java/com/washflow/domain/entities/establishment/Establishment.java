package com.washflow.domain.entities.establishment;

import java.time.Instant;

/**
 * {@code estabelecimento}: the single car wash this installation serves. Hours are whole shop-local
 * (America/Manaus) hours, {@code closingHour} exclusive. {@code configuredAt} is null until the
 * owner completes the setup wizard.
 */
public record Establishment(
    String name,
    int openingHour,
    int closingHour,
    boolean openOnSunday,
    int maxVehiclesPerHour,
    Instant configuredAt) {

  public boolean isConfigured() {
    return configuredAt != null;
  }
}
