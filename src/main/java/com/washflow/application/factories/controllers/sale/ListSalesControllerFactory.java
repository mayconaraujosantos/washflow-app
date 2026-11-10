package com.washflow.application.factories.controllers.sale;

import com.washflow.application.factories.usecases.sale.ListSalesFactory;
import com.washflow.presentation.controllers.listsales.ListSalesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListSalesControllerFactory {

  private ListSalesControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListSalesController(ListSalesFactory.make(jdbi));
  }
}
