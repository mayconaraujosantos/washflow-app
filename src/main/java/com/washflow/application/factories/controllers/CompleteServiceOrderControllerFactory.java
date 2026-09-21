package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CompleteServiceOrderFactory;
import com.washflow.presentation.controllers.completeserviceorder.CompleteServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CompleteServiceOrderControllerFactory {

  private CompleteServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CompleteServiceOrderController(CompleteServiceOrderFactory.make(jdbi));
  }
}
