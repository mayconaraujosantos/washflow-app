package com.washflow.presentation.controllers.user;

import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * Password reset by e-mail code:
 *
 * <ul>
 *   <li>{@code POST /api/auth/customers/password-reset} {@code {"phone"}} and {@code
 *       /api/auth/managers/password-reset} {@code {"email"}}: 202 {@code {"expiresInSeconds"}}, the
 *       same whether the account exists;
 *   <li>{@code .../password-reset/confirm} with {@code code} and {@code password}: 200 {@code
 *       {"passwordChanged": true}}, or 401 for a wrong/expired code (counted by the login
 *       throttle).
 * </ul>
 */
public class PasswordResetController implements Controller {

  public enum Who {
    CUSTOMER,
    MANAGER
  }

  private final ResetPasswordByEmail reset;
  private final Who who;
  private final boolean confirm;

  private PasswordResetController(ResetPasswordByEmail reset, Who who, boolean confirm) {
    this.reset = reset;
    this.who = who;
    this.confirm = confirm;
  }

  public static PasswordResetController request(ResetPasswordByEmail reset, Who who) {
    return new PasswordResetController(reset, who, false);
  }

  public static PasswordResetController confirm(ResetPasswordByEmail reset, Who who) {
    return new PasswordResetController(reset, who, true);
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      String account = BodyValues.text(body, who == Who.CUSTOMER ? "phone" : "email");
      if (confirm) {
        String code = BodyValues.text(body, "code");
        String password = BodyValues.text(body, "password");
        if (who == Who.CUSTOMER) {
          reset.confirmForCustomer(account, code, password);
        } else {
          reset.confirmForManager(account, code, password);
        }
        return HttpHelper.ok(Map.of("passwordChanged", true));
      }
      int seconds =
          who == Who.CUSTOMER
              ? reset.requestForCustomer(account)
              : reset.requestForManager(account);
      return new HttpResponse(202, Map.of("expiresInSeconds", seconds));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (InvalidOtpError e) {
      return HttpHelper.unauthorized(e);
    } catch (EmailResetUnavailableError | EmailDeliveryFailedError e) {
      return new HttpResponse(503, HttpHelper.errorBody(e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
