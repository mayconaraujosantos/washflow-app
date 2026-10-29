package com.washflow.presentation.controllers.updateuseractive;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.UserHasActiveWashesError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserActive;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * Parses the HTTP request for updating a user's active status and delegates to {@code
 * UpdateUserActive}. Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase
 * interface.
 */
public class UpdateUserActiveController implements Controller {

  private final UpdateUserActive updateUserActive;

  public UpdateUserActiveController(UpdateUserActive updateUserActive) {
    this.updateUserActive = updateUserActive;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID userId = UUID.fromString(request.pathParams().get("id"));

      @SuppressWarnings("unchecked")
      Map<String, Object> body = request.body();
      Boolean active = (Boolean) body.get("active");
      if (active == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("Provide the active field"));
      }

      UpdateUserActive.Params params = new UpdateUserActive.Params(userId, active);

      User user = updateUserActive.update(params);
      return new HttpResponse(200, user);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return new HttpResponse(404, Map.of("error", "User not found"));
    } catch (UserHasActiveWashesError e) {
      return new HttpResponse(
          409, Map.of("error", "Complete or reassign this washer's active orders first"));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
