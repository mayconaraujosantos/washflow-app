package com.washflow.application.factories.controllers.user;

import com.washflow.application.factories.usecases.user.UpdateUserActiveFactory;
import com.washflow.presentation.controllers.user.UpdateUserActiveController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class UpdateUserActiveControllerFactory {

  private UpdateUserActiveControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new UpdateUserActiveController(UpdateUserActiveFactory.make(jdbi));
  }
}
