package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.NotFoundError;
import java.util.UUID;

public class ServiceOrderNotFoundError extends NotFoundError {

  public ServiceOrderNotFoundError(UUID serviceOrderId) {
    super("SERVICE_ORDER_NOT_FOUND", "Service order not found: " + serviceOrderId);
  }
}
