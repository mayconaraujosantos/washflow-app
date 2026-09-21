package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CreateVehicleFactory;
import com.washflow.presentation.controllers.createvehicle.CreateVehicleController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public final class CreateVehicleControllerFactory {

  private CreateVehicleControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("customerId"), new RequiredFieldValidation("plate")));

    return new CreateVehicleController(CreateVehicleFactory.make(jdbi), validation);
  }
}
