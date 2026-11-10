package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.pricing.DbCreateServicePrice;
import com.washflow.domain.usecases.pricing.CreateServicePrice;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CreateServicePriceFactory {

  private CreateServicePriceFactory() {}

  public static CreateServicePrice make(Jdbi jdbi) {
    return new DbCreateServicePrice(new ServicePriceJdbiRepository(jdbi));
  }
}
