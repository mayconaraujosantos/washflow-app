package com.washflow.domain.errors.establishment;

public class EstablishmentNotFoundError extends Exception {

  public EstablishmentNotFoundError() {
    super("Lava-jato não encontrado");
  }
}
