package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.AuthenticateUserFactory;
import com.washflow.domain.entities.UserProfile;
import com.washflow.presentation.controllers.authenticateuser.AuthenticateUserController;
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

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(new RequiredFieldValidation("phone"), new RequiredFieldValidation("name")));

    return new AuthenticateUserController(
        AuthenticateUserFactory.make(jdbi), validation, UserProfile.CUSTOMER);
  }
}
