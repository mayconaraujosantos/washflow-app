package com.washflow.application.factories.controllers.sale;

import com.washflow.application.factories.usecases.sale.RefundSaleFactory;
import com.washflow.presentation.controllers.refundsale.RefundSaleController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class RefundSaleControllerFactory {

  private RefundSaleControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new RefundSaleController(RefundSaleFactory.make(jdbi));
  }
}
