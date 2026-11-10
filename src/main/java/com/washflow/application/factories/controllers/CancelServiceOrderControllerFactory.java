package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CancelServiceOrderFactory;
import com.washflow.presentation.controllers.cancelserviceorder.CancelServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CancelServiceOrderControllerFactory {

  private CancelServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CancelServiceOrderController(CancelServiceOrderFactory.make(jdbi));
  }
}
