package com.washflow.presentation.controllers.authenticateuser;

import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.errors.user.WasherSelfRegistrationNotAllowedError;
import com.washflow.domain.usecases.user.AuthenticateUser;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;

/**
 * One instance per QR code / role - {@code profile} is fixed at construction time by whichever
 * {@code application.factories.controllers} factory wires up this controller for a given route
 * (e.g. {@code AuthenticateManagerControllerFactory} always passes {@link UserProfile#MANAGER}),
 * never read from the request body. That's what makes a phone unable to self-assign GERENTE by
 * simply editing a JSON field: the profile is determined by which URL was called, not by anything
 * the caller sends.
 */
public class AuthenticateUserController implements Controller {

  private final AuthenticateUser authenticateUser;
  private final Validation validation;
  private final UserProfile profile;

  public AuthenticateUserController(
      AuthenticateUser authenticateUser, Validation validation, UserProfile profile) {
    this.authenticateUser = authenticateUser;
    this.validation = validation;
    this.profile = profile;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      String phone = (String) request.body().get("phone");
      String name = (String) request.body().get("name");
      String password = (String) request.body().get("password");
      // Managers may log in with e-mail instead of phone (see
      // AuthenticateManagerControllerFactory).
      String email = profile == UserProfile.MANAGER ? (String) request.body().get("email") : null;

      if (password.length() < 8) {
        return HttpHelper.badRequest(
            new IllegalArgumentException("Senha deve ter no mínimo 8 caracteres"));
      }
      var user =
          authenticateUser.authenticate(
              new AuthenticateUser.Params(phone, name, password, profile, email));

      return HttpHelper.ok(user);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (InvalidCredentialsError e) {
      return HttpHelper.unauthorized(e);
    } catch (UserProfileMismatchError e) {
      return HttpHelper.conflict(e);
    } catch (ManagerSelfRegistrationNotAllowedError | WasherSelfRegistrationNotAllowedError e) {
      return HttpHelper.forbidden(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
