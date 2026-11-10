package com.washflow.domain.errors.vehicle;

/** The plate is not a valid Brazilian plate (old {@code ABC1234} or Mercosul {@code ABC1D23}). */
public class InvalidPlateError extends Exception {

  public InvalidPlateError(String plate) {
    super("Placa inválida: " + plate);
  }
}
