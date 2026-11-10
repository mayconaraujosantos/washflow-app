package com.washflow.application.factories.controllers.user;

import com.washflow.application.factories.usecases.user.ListUsersFactory;
import com.washflow.presentation.controllers.user.ListUsersController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListUsersControllerFactory {

  private ListUsersControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListUsersController(ListUsersFactory.make(jdbi));
  }
}
