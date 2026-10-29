package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.SaveServiceOrderChecklistFactory;
import com.washflow.presentation.controllers.savechecklist.SaveChecklistController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class SaveChecklistControllerFactory {

  private SaveChecklistControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new SaveChecklistController(SaveServiceOrderChecklistFactory.make(jdbi));
  }
}
