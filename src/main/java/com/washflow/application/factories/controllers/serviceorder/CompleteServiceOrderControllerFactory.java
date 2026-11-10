package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.CompleteServiceOrderFactory;
import com.washflow.presentation.controllers.completeserviceorder.CompleteServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CompleteServiceOrderControllerFactory {

  private CompleteServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CompleteServiceOrderController(CompleteServiceOrderFactory.make(jdbi));
  }
}
