package com.washflow.domain.errors.common;

public class ServicePriceNotFoundError extends Exception {

  public ServicePriceNotFoundError(int servicePriceId) {
    super("Service price not found: " + servicePriceId);
  }
}
