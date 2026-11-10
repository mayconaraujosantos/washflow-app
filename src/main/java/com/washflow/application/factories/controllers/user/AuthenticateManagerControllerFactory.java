package com.washflow.application.factories.controllers.user;

import com.washflow.application.AppConfig;
import com.washflow.application.factories.usecases.user.AuthenticateUserFactory;
import com.washflow.domain.entities.UserProfile;
import com.washflow.presentation.controllers.authenticateuser.AuthenticateUserController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.AnyRequiredFieldValidation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** Backs the GERENTE QR code - {@link UserProfile#MANAGER} is fixed here, never client-supplied. */
public final class AuthenticateManagerControllerFactory {

  private AuthenticateManagerControllerFactory() {}

  public static Controller make(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new AnyRequiredFieldValidation("email", "phone"),
                new RequiredFieldValidation("password")));

    return new AuthenticateUserController(
        AuthenticateUserFactory.make(jdbi, appConfig), validation, UserProfile.MANAGER);
  }
}
