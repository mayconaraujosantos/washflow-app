package com.washflow.application.factories.controllers.sale;

import com.washflow.application.factories.usecases.sale.CreateSaleFactory;
import com.washflow.presentation.controllers.createsale.CreateSaleController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateSaleControllerFactory {

  private CreateSaleControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateSaleController(CreateSaleFactory.make(jdbi));
  }
}
