package com.washflow.presentation.controllers.customer;

import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/**
 * {@code GET /api/auth/options}: which passwordless sign-ins (WhatsApp code, Google) are enabled.
 */
public class CustomerLoginOptionsController implements Controller {

  private final CustomerPasswordlessLogin login;

  public CustomerLoginOptionsController(CustomerPasswordlessLogin login) {
    this.login = login;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(login.options());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
