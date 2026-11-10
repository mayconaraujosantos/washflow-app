package com.washflow.domain.errors.vehicle;

import java.util.UUID;

public class VehicleNotFoundError extends Exception {

  public VehicleNotFoundError(UUID vehicleId) {
    super("Vehicle not found: " + vehicleId);
  }
}
