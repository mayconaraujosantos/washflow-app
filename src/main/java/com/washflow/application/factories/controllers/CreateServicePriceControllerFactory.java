package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CreateServicePriceFactory;
import com.washflow.presentation.controllers.createserviceprice.CreateServicePriceController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateServicePriceControllerFactory {

  private CreateServicePriceControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateServicePriceController(CreateServicePriceFactory.make(jdbi));
  }
}
