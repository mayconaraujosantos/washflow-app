package com.washflow.application.factories.controllers.pricing;

import com.washflow.application.factories.usecases.pricing.ListServicePricesFactory;
import com.washflow.presentation.controllers.listserviceprices.ListServicePricesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListServicePricesControllerFactory {

  private ListServicePricesControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListServicePricesController(ListServicePricesFactory.make(jdbi));
  }
}
