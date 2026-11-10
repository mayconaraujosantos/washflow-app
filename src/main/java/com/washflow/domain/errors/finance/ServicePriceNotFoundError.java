package com.washflow.domain.errors.finance;

public class ServicePriceNotFoundError extends Exception {

  public ServicePriceNotFoundError(int servicePriceId) {
    super("Service price not found: " + servicePriceId);
  }
}
