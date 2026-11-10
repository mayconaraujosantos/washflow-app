package com.washflow.domain.errors.establishment;

public class EstablishmentSlugTakenError extends Exception {

  public EstablishmentSlugTakenError(String slug) {
    super("Já existe um lava-jato com o endereço " + slug);
  }
}
