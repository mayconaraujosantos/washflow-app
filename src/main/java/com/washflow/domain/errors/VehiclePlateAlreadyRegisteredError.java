package com.washflow.domain.errors;

public class VehiclePlateAlreadyRegisteredError extends Exception {

  public VehiclePlateAlreadyRegisteredError(String plate) {
    super("Vehicle plate already registered: " + plate);
  }
}
