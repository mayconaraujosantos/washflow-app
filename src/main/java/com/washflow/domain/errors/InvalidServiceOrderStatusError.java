package com.washflow.domain.errors;

import com.washflow.domain.entities.ServiceOrderStatus;

public class InvalidServiceOrderStatusError extends Exception {

  public InvalidServiceOrderStatusError(ServiceOrderStatus current, ServiceOrderStatus expected) {
    super("Expected service order status " + expected + " but found " + current);
  }
}
