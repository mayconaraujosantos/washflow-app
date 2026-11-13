package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

/** A washer works one car at a time: the wash in progress (or paused) comes first. */
public class WasherBusyError extends ConflictError {

  public WasherBusyError() {
    super(
        "WASHER_BUSY",
        "Você já tem uma lavagem em andamento. Conclua ou retome essa antes de iniciar outra.");
  }
}
