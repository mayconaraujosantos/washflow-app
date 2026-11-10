package com.washflow.application.factories.controllers.sale;

import com.washflow.application.factories.usecases.sale.ListSalesFactory;
import com.washflow.presentation.controllers.sale.ListSaleRefundsController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListSaleRefundsControllerFactory {

  private ListSaleRefundsControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListSaleRefundsController(ListSalesFactory.make(jdbi));
  }
}
