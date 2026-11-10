package com.washflow.application.factories.controllers.yard;

import com.washflow.application.AppConfig;
import com.washflow.application.factories.usecases.yard.RegisterVehicleEntryFactory;
import com.washflow.presentation.controllers.yard.RegisterVehicleEntryController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class RegisterVehicleEntryControllerFactory {

  private RegisterVehicleEntryControllerFactory() {}

  public static Controller make(Jdbi jdbi, AppConfig appConfig) {
    return new RegisterVehicleEntryController(RegisterVehicleEntryFactory.make(jdbi, appConfig));
  }
}
