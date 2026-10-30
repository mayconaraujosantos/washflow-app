package com.washflow.presentation.controllers.listusers;

import com.washflow.domain.entities.User;
import com.washflow.domain.usecases.ListUsers;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;

/**
 * Returns all users. Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase
 * interface.
 */
public class ListUsersController implements Controller {

  private final ListUsers listUsers;

  public ListUsersController(ListUsers listUsers) {
    this.listUsers = listUsers;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      List<User> users = listUsers.list();
      return new HttpResponse(200, users);
    } catch (Exception e) {
      return new HttpResponse(500, java.util.Map.of("error", e.getMessage()));
    }
  }
}
