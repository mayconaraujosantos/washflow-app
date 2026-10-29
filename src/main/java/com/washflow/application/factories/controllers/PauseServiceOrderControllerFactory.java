package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.PauseServiceOrderFactory;
import com.washflow.presentation.controllers.pauseserviceorder.PauseServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class PauseServiceOrderControllerFactory {

  private PauseServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new PauseServiceOrderController(PauseServiceOrderFactory.make(jdbi));
  }
}
