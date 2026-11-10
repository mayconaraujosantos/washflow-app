package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;

public class InvalidServiceOrderStatusError extends Exception {

  public InvalidServiceOrderStatusError(ServiceOrderStatus current, ServiceOrderStatus expected) {
    super("Expected service order status " + expected + " but found " + current);
  }
}
