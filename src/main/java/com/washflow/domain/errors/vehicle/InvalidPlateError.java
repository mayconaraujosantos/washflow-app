package com.washflow.domain.errors.vehicle;

import com.washflow.domain.errors.common.ValidationError;

/** The plate is not a valid Brazilian plate (old {@code ABC1234} or Mercosul {@code ABC1D23}). */
public class InvalidPlateError extends ValidationError {

  public InvalidPlateError(String plate) {
    super("INVALID_PLATE", "Placa inválida: " + plate);
  }
}
