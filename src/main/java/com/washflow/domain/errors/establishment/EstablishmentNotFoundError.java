package com.washflow.domain.errors.establishment;

import com.washflow.domain.errors.common.NotFoundError;

public class EstablishmentNotFoundError extends NotFoundError {

  public EstablishmentNotFoundError() {
    super("ESTABLISHMENT_NOT_FOUND", "Lava-jato não encontrado");
  }
}
