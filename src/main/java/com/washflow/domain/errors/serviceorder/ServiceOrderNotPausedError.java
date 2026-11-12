package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ConflictError;

public class ServiceOrderNotPausedError extends ConflictError {

  public ServiceOrderNotPausedError() {
    super("SERVICE_ORDER_NOT_PAUSABLE", "Service order is not an active wash assigned to you");
  }
}
