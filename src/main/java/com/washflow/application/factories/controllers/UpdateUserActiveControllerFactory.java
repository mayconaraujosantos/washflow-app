package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.UpdateUserActiveFactory;
import com.washflow.presentation.controllers.updateuseractive.UpdateUserActiveController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class UpdateUserActiveControllerFactory {

  private UpdateUserActiveControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new UpdateUserActiveController(UpdateUserActiveFactory.make(jdbi));
  }
}
