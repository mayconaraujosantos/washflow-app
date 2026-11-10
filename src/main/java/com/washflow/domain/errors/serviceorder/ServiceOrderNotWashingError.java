package com.washflow.domain.errors.serviceorder;

public class ServiceOrderNotWashingError extends Exception {

  public ServiceOrderNotWashingError() {
    super("Service order is not paused or is not assigned to you");
  }
}
