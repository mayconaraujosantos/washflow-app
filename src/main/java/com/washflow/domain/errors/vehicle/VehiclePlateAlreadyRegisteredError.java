package com.washflow.domain.errors.vehicle;

import com.washflow.domain.errors.common.ConflictError;

public class VehiclePlateAlreadyRegisteredError extends ConflictError {

  public VehiclePlateAlreadyRegisteredError(String plate) {
    super(
        "VEHICLE_PLATE_ALREADY_REGISTERED",
        "A placa "
            + plate
            + " já está cadastrada neste lava-jato. Se o carro é seu, fale com o balcão.");
  }
}
