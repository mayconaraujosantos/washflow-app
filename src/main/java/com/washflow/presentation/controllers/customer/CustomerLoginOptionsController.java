package com.washflow.presentation.controllers.customer;

import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code GET /api/auth/options}: which passwordless sign-ins (WhatsApp code, Google) are enabled,
 * and {@code emailCode}: the e-mail channel is on, so the customer can both sign in with a code by
 * e-mail and reset a forgotten password by e-mail (one channel serves both, see
 * UserModule.emailSender).
 */
public class CustomerLoginOptionsController implements Controller {

  private final CustomerPasswordlessLogin login;
  private final ResetPasswordByEmail passwordReset;

  public CustomerLoginOptionsController(
      CustomerPasswordlessLogin login, ResetPasswordByEmail passwordReset) {
    this.login = login;
    this.passwordReset = passwordReset;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      CustomerPasswordlessLogin.Options options = login.options();
      Map<String, Object> body = new HashMap<>();
      body.put("whatsappCode", options.whatsappCode());
      body.put("googleClientId", options.googleClientId());
      body.put("emailCode", options.emailCode() && passwordReset.available());
      return HttpHelper.ok(body);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
