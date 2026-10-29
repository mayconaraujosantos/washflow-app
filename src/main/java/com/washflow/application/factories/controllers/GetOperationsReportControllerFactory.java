package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.GetOperationsReportFactory;
import com.washflow.presentation.controllers.getoperationsreport.GetOperationsReportController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetOperationsReportControllerFactory {

  private GetOperationsReportControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetOperationsReportController(GetOperationsReportFactory.make(jdbi));
  }
}
