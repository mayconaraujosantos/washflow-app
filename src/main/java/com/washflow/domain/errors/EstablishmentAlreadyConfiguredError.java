package com.washflow.domain.errors;

public class EstablishmentAlreadyConfiguredError extends Exception {

  public EstablishmentAlreadyConfiguredError() {
    super("A plataforma já foi configurada");
  }
}
