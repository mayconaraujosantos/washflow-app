package com.washflow.presentation.controllers.updateuserprofile;

import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ForbiddenError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserProfile;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.util.UUID;

public class UpdateUserProfileController implements Controller {

  private final UpdateUserProfile updateUserProfile;
  private final Validation validation;

  public UpdateUserProfileController(UpdateUserProfile updateUserProfile, Validation validation) {
    this.updateUserProfile = updateUserProfile;
    this.validation = validation;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      UUID targetUserId = UUID.fromString(request.pathParams().get("id"));
      UUID requestedByUserId = UUID.fromString((String) request.body().get("managerId"));
      UserProfile newProfile = UserProfile.valueOf((String) request.body().get("newProfile"));

      var user =
          updateUserProfile.update(
              new UpdateUserProfile.Params(requestedByUserId, targetUserId, newProfile));

      return HttpHelper.ok(user);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (ForbiddenError e) {
      return HttpHelper.forbidden(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
