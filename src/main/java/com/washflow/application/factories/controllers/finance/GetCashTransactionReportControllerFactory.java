package com.washflow.application.factories.controllers.finance;

import com.washflow.application.factories.usecases.finance.GetCashTransactionReportFactory;
import com.washflow.presentation.controllers.getcashtransactionreport.GetCashTransactionReportController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetCashTransactionReportControllerFactory {

  private GetCashTransactionReportControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetCashTransactionReportController(GetCashTransactionReportFactory.make(jdbi));
  }
}
