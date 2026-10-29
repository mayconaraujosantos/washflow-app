package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListUsersFactory;
import com.washflow.presentation.controllers.listusers.ListUsersController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListUsersControllerFactory {

  private ListUsersControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListUsersController(ListUsersFactory.make(jdbi));
  }
}
