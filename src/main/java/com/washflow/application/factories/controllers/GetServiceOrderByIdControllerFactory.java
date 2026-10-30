package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.GetServiceOrderByIdFactory;
import com.washflow.presentation.controllers.getserviceorderbyid.GetServiceOrderByIdController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetServiceOrderByIdControllerFactory {

  private GetServiceOrderByIdControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetServiceOrderByIdController(GetServiceOrderByIdFactory.make(jdbi));
  }
}
