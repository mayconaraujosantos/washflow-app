package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ValidationError;

/** The account picked to wash the car isn't an active washer of this lava-jato. */
public class WasherNotAssignableError extends ValidationError {

  public WasherNotAssignableError() {
    super("WASHER_NOT_ASSIGNABLE", "Escolha um lavador ativo deste lava-jato.");
  }
}
