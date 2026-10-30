package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.GetWashingByWasherFactory;
import com.washflow.presentation.controllers.getwashingbywasher.GetWashingByWasherController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetWashingByWasherControllerFactory {

  private GetWashingByWasherControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetWashingByWasherController(GetWashingByWasherFactory.make(jdbi));
  }
}
