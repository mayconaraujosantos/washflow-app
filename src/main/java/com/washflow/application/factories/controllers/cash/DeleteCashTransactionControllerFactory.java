package com.washflow.application.factories.controllers.cash;

import com.washflow.application.factories.usecases.cash.DeleteCashTransactionFactory;
import com.washflow.presentation.controllers.deletecashtransaction.DeleteCashTransactionController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class DeleteCashTransactionControllerFactory {

  private DeleteCashTransactionControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new DeleteCashTransactionController(DeleteCashTransactionFactory.make(jdbi));
  }
}
