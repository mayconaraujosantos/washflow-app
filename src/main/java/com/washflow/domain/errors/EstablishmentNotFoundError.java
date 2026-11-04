package com.washflow.domain.errors;

public class EstablishmentNotFoundError extends Exception {

  public EstablishmentNotFoundError() {
    super("Lava-jato não encontrado");
  }
}
