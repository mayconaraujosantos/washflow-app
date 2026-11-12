package com.washflow.domain.errors.pricing;

import com.washflow.domain.errors.common.NotFoundError;

public class ServicePriceNotFoundError extends NotFoundError {

  public ServicePriceNotFoundError(int servicePriceId) {
    super("SERVICE_PRICE_NOT_FOUND", "Service price not found: " + servicePriceId);
  }
}
