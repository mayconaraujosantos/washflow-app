package com.washflow.presentation.controllers.customer;

import com.washflow.domain.errors.auth.GoogleLoginUnavailableError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/** {@code POST /api/auth/customers/google}: signs the customer in with a Google ID token. */
public class GoogleCustomerLoginController implements Controller {

  private final CustomerPasswordlessLogin login;

  public GoogleCustomerLoginController(CustomerPasswordlessLogin login) {
    this.login = login;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(login.signInWithGoogle(BodyValues.text(body, "credential")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (GoogleLoginUnavailableError e) {
      return new HttpResponse(503, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
