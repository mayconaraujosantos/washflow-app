package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CreateCashTransactionFactory;
import com.washflow.presentation.controllers.createcashtransaction.CreateCashTransactionController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateCashTransactionControllerFactory {

  private CreateCashTransactionControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateCashTransactionController(CreateCashTransactionFactory.make(jdbi));
  }
}
