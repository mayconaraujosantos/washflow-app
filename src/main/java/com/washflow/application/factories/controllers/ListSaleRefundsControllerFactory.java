package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListSalesFactory;
import com.washflow.presentation.controllers.listsales.ListSaleRefundsController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListSaleRefundsControllerFactory {

  private ListSaleRefundsControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListSaleRefundsController(ListSalesFactory.make(jdbi));
  }
}
