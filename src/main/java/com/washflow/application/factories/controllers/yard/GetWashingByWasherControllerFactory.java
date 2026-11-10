package com.washflow.application.factories.controllers.yard;

import com.washflow.application.factories.usecases.yard.GetWashingByWasherFactory;
import com.washflow.presentation.controllers.yard.GetWashingByWasherController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetWashingByWasherControllerFactory {

  private GetWashingByWasherControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetWashingByWasherController(GetWashingByWasherFactory.make(jdbi));
  }
}
