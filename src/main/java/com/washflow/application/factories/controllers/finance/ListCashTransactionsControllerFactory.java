package com.washflow.application.factories.controllers.finance;

import com.washflow.application.factories.usecases.finance.ListCashTransactionsFactory;
import com.washflow.presentation.controllers.finance.ListCashTransactionsController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListCashTransactionsControllerFactory {

  private ListCashTransactionsControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListCashTransactionsController(ListCashTransactionsFactory.make(jdbi));
  }
}
