package com.washflow.domain.errors;

public class EstablishmentAlreadyConfiguredError extends Exception {

  public EstablishmentAlreadyConfiguredError() {
    super("O estabelecimento já foi configurado");
  }
}
