package com.washflow.domain.errors;

/** The vehicle already has an open service order in the yard (waiting, washing or ready). */
public class VehicleAlreadyInYardError extends Exception {

  public VehicleAlreadyInYardError(String plate) {
    super(
        "Este veículo"
            + (plate == null || plate.isBlank() ? "" : " (" + plate + ")")
            + " já está no pátio. Finalize o atendimento atual antes de registrar outra entrada.");
  }
}
