package com.washflow.domain.errors.serviceorder;

import java.util.UUID;

public class ServiceOrderNotFoundError extends Exception {

  public ServiceOrderNotFoundError(UUID serviceOrderId) {
    super("Service order not found: " + serviceOrderId);
  }
}
