package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CreateWasherFactory;
import com.washflow.presentation.controllers.createwasher.CreateWasherController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateWasherControllerFactory {

  private CreateWasherControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateWasherController(CreateWasherFactory.make(jdbi));
  }
}
