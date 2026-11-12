package com.washflow.domain.errors.vehicle;

import com.washflow.domain.errors.common.ConflictError;

/** The vehicle already has an open service order in the yard (waiting, washing or ready). */
public class VehicleAlreadyInYardError extends ConflictError {

  public VehicleAlreadyInYardError(String plate) {
    super(
        "VEHICLE_ALREADY_IN_YARD",
        "Este veículo"
            + (plate == null || plate.isBlank() ? "" : " (" + plate + ")")
            + " já está no pátio. Finalize o atendimento atual antes de registrar outra entrada.");
  }
}
