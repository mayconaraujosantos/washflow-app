package com.washflow.presentation.controllers.user;

import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.UpdateUserEmail;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

/** {@code PATCH /api/users/{id}/email} with {@code {"email": "..."}} (null/blank clears it). */
public class UpdateUserEmailController implements Controller {

  private final UpdateUserEmail updateUserEmail;

  public UpdateUserEmailController(UpdateUserEmail updateUserEmail) {
    this.updateUserEmail = updateUserEmail;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID userId = UUID.fromString(request.pathParams().get("id"));
      Object email = request.body() == null ? null : request.body().get("email");
      return HttpHelper.ok(updateUserEmail.update(userId, email == null ? null : email.toString()));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (EmailAlreadyRegisteredError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
