package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.UpdateCashTransactionFactory;
import com.washflow.presentation.controllers.updatecashtransaction.UpdateCashTransactionController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class UpdateCashTransactionControllerFactory {

  private UpdateCashTransactionControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new UpdateCashTransactionController(UpdateCashTransactionFactory.make(jdbi));
  }
}
