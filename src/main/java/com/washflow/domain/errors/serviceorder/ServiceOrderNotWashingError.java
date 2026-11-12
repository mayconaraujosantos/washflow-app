package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

public class ServiceOrderNotWashingError extends ConflictError {

  public ServiceOrderNotWashingError() {
    super("SERVICE_ORDER_NOT_RESUMABLE", "Service order is not paused or is not assigned to you");
  }
}
