package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.FinalizeServiceOrderFactory;
import com.washflow.presentation.controllers.finalizeserviceorder.FinalizeServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class FinalizeServiceOrderControllerFactory {

  private FinalizeServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new FinalizeServiceOrderController(FinalizeServiceOrderFactory.make(jdbi));
  }
}
