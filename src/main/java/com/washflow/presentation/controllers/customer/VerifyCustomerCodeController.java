package com.washflow.presentation.controllers.customer;

import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/auth/customers/otp/verify}: signs in with the code sent to the {@code email} or
 * {@code phone}; a new account also needs the name.
 */
public class VerifyCustomerCodeController implements Controller {

  private final CustomerPasswordlessLogin login;

  public VerifyCustomerCodeController(CustomerPasswordlessLogin login) {
    this.login = login;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    OtpRecipient recipient;
    String code;
    String name;
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      recipient = OtpRecipients.from(body);
      code = BodyValues.text(body, "code");
      name = BodyValues.text(body, "name");
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    }
    try {
      return HttpHelper.ok(login.verifyCode(recipient, code, name));
    } catch (IllegalArgumentException e) {
      // Valid code for a new account: the app asks for the customer's name.
      return new HttpResponse(400, Map.of("error", e.getMessage(), "needsName", true));
    } catch (InvalidOtpError e) {
      return HttpHelper.unauthorized(e);
    } catch (UserProfileMismatchError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
