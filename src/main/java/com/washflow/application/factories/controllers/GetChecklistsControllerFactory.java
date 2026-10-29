package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListServiceOrderChecklistsFactory;
import com.washflow.presentation.controllers.getchecklists.GetChecklistsController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetChecklistsControllerFactory {

  private GetChecklistsControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetChecklistsController(ListServiceOrderChecklistsFactory.make(jdbi));
  }
}
