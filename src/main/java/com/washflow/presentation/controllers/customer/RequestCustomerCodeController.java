package com.washflow.presentation.controllers.customer;

import com.washflow.domain.errors.auth.OtpTooSoonError;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/auth/customers/otp}: sends a 6-digit code to the customer's {@code email} or, by
 * WhatsApp, to the {@code phone}.
 */
public class RequestCustomerCodeController implements Controller {

  private final CustomerPasswordlessLogin login;

  public RequestCustomerCodeController(CustomerPasswordlessLogin login) {
    this.login = login;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(Map.of("expiresInSeconds", login.requestCode(OtpRecipients.from(body))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (OtpTooSoonError e) {
      return new HttpResponse(429, Map.of("error", e.getMessage()));
    } catch (OtpUnavailableError e) {
      return new HttpResponse(503, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
