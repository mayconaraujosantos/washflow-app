package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.UpdateUserProfileFactory;
import com.washflow.presentation.controllers.updateuserprofile.UpdateUserProfileController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public final class UpdateUserProfileControllerFactory {

  private UpdateUserProfileControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("managerId"),
                new RequiredFieldValidation("newProfile")));

    return new UpdateUserProfileController(UpdateUserProfileFactory.make(jdbi), validation);
  }
}
