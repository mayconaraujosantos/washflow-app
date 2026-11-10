package com.washflow.application.factories.controllers.user;

import com.washflow.application.factories.usecases.user.CreateWasherFactory;
import com.washflow.presentation.controllers.user.CreateWasherController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateWasherControllerFactory {

  private CreateWasherControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateWasherController(CreateWasherFactory.make(jdbi));
  }
}
