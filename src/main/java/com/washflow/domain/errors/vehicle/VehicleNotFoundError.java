package com.washflow.domain.errors.vehicle;

import com.washflow.domain.errors.common.NotFoundError;
import java.util.UUID;

public class VehicleNotFoundError extends NotFoundError {

  public VehicleNotFoundError(UUID vehicleId) {
    super("VEHICLE_NOT_FOUND", "Vehicle not found: " + vehicleId);
  }
}
