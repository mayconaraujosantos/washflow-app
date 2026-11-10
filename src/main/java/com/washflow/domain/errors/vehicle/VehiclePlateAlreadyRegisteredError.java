package com.washflow.domain.errors.vehicle;

public class VehiclePlateAlreadyRegisteredError extends Exception {

  public VehiclePlateAlreadyRegisteredError(String plate) {
    super("Vehicle plate already registered: " + plate);
  }
}
