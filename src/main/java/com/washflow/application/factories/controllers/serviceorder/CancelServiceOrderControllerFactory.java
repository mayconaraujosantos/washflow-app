package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.CancelServiceOrderFactory;
import com.washflow.presentation.controllers.serviceorder.CancelServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CancelServiceOrderControllerFactory {

  private CancelServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CancelServiceOrderController(CancelServiceOrderFactory.make(jdbi));
  }
}
