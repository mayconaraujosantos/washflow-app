package com.washflow.presentation.controllers.customer;

import com.washflow.domain.usecases.customer.CustomerRecoveryEmail;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code GET} and {@code PATCH /api/customers/{id}/recovery-email} ({@code {"email": "..."}}, blank
 * clears it): answers {@code {"email": ...}}. The middleware lets customers reach only their own.
 */
public class CustomerRecoveryEmailController implements Controller {

  private final CustomerRecoveryEmail recoveryEmail;
  private final boolean update;

  private CustomerRecoveryEmailController(CustomerRecoveryEmail recoveryEmail, boolean update) {
    this.recoveryEmail = recoveryEmail;
    this.update = update;
  }

  public static CustomerRecoveryEmailController load(CustomerRecoveryEmail recoveryEmail) {
    return new CustomerRecoveryEmailController(recoveryEmail, false);
  }

  public static CustomerRecoveryEmailController update(CustomerRecoveryEmail recoveryEmail) {
    return new CustomerRecoveryEmailController(recoveryEmail, true);
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID customerId = UUID.fromString(request.pathParams().get("id"));
      Optional<String> email;
      if (update) {
        Map<String, Object> body = request.body() == null ? Map.of() : request.body();
        email = recoveryEmail.update(customerId, BodyValues.text(body, "email"));
      } else {
        email = recoveryEmail.load(customerId);
      }
      Map<String, Object> response = new HashMap<>();
      response.put("email", email.orElse(null));
      return HttpHelper.ok(response);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
