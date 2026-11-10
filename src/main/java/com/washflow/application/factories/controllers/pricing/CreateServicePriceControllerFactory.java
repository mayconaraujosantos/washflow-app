package com.washflow.application.factories.controllers.pricing;

import com.washflow.application.factories.usecases.pricing.CreateServicePriceFactory;
import com.washflow.presentation.controllers.pricing.CreateServicePriceController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateServicePriceControllerFactory {

  private CreateServicePriceControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateServicePriceController(CreateServicePriceFactory.make(jdbi));
  }
}
