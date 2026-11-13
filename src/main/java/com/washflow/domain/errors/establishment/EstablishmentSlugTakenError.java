package com.washflow.domain.errors.establishment;

import com.washflow.domain.errors.common.ConflictError;

public class EstablishmentSlugTakenError extends ConflictError {

  public EstablishmentSlugTakenError(String slug) {
    super("ESTABLISHMENT_SLUG_TAKEN", "Já existe um lava-jato com o endereço " + slug);
  }
}
