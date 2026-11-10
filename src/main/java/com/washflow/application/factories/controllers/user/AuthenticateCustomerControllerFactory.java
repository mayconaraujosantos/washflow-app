package com.washflow.application.factories.controllers.user;

import com.washflow.application.AppConfig;
import com.washflow.application.factories.usecases.user.AuthenticateUserFactory;
import com.washflow.domain.entities.UserProfile;
import com.washflow.presentation.controllers.user.AuthenticateUserController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/**
 * Backs the CLIENTE QR code - {@link UserProfile#CUSTOMER} is fixed here, never client-supplied.
 */
public final class AuthenticateCustomerControllerFactory {

  private AuthenticateCustomerControllerFactory() {}

  public static Controller make(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(
                // "name" is only needed to create the account - DbAuthenticateUser checks it
                // there, so returning customers can sign in with phone and password.
                new RequiredFieldValidation("phone"), new RequiredFieldValidation("password")));

    return new AuthenticateUserController(
        AuthenticateUserFactory.make(jdbi, appConfig), validation, UserProfile.CUSTOMER);
  }
}
