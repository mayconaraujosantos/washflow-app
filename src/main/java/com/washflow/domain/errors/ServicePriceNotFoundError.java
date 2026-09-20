package com.washflow.domain.errors;

public class ServicePriceNotFoundError extends Exception {

  public ServicePriceNotFoundError(int servicePriceId) {
    super("Service price not found: " + servicePriceId);
  }
}
