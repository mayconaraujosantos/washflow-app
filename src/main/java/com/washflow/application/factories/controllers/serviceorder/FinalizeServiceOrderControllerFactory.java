package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.FinalizeServiceOrderFactory;
import com.washflow.presentation.controllers.serviceorder.FinalizeServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class FinalizeServiceOrderControllerFactory {

  private FinalizeServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new FinalizeServiceOrderController(FinalizeServiceOrderFactory.make(jdbi));
  }
}
