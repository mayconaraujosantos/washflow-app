package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.errors.common.ConflictError;

public class InvalidServiceOrderStatusError extends ConflictError {

  public InvalidServiceOrderStatusError(ServiceOrderStatus current, ServiceOrderStatus expected) {
    super(
        "INVALID_SERVICE_ORDER_STATUS",
        "Expected service order status " + expected + " but found " + current);
  }
}
