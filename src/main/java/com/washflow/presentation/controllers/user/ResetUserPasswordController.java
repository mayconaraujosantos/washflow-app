package com.washflow.presentation.controllers.user;

import com.washflow.domain.errors.user.PasswordResetNotAllowedError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.ResetUserPassword;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/** {@code PATCH /api/users/{id}/password} with {@code {"password": "..."}} (manager only). */
public class ResetUserPasswordController implements Controller {

  private final ResetUserPassword resetUserPassword;

  public ResetUserPasswordController(ResetUserPassword resetUserPassword) {
    this.resetUserPassword = resetUserPassword;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID userId = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(resetUserPassword.reset(userId, BodyValues.text(body, "password")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (PasswordResetNotAllowedError e) {
      return HttpHelper.forbidden(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
