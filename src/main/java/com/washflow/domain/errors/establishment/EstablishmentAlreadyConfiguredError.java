package com.washflow.domain.errors.establishment;

import com.washflow.domain.errors.common.ConflictError;

public class EstablishmentAlreadyConfiguredError extends ConflictError {

  public EstablishmentAlreadyConfiguredError() {
    super("PLATFORM_ALREADY_CONFIGURED", "A plataforma já foi configurada");
  }
}
