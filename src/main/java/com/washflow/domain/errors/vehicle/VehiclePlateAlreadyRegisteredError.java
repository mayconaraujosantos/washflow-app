package com.washflow.domain.errors.vehicle;

public class VehiclePlateAlreadyRegisteredError extends Exception {

  public VehiclePlateAlreadyRegisteredError(String plate) {
    super(
        "A placa "
            + plate
            + " já está cadastrada neste lava-jato. Se o carro é seu, fale com o balcão.");
  }
}
