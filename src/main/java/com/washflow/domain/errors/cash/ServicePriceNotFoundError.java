package com.washflow.domain.errors.cash;

public class ServicePriceNotFoundError extends Exception {

  public ServicePriceNotFoundError(int servicePriceId) {
    super("Service price not found: " + servicePriceId);
  }
}
