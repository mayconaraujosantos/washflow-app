package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.StartServiceOrderFactory;
import com.washflow.presentation.controllers.serviceorder.StartServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public final class StartServiceOrderControllerFactory {

  private StartServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(List.of(new RequiredFieldValidation("washerId")));

    return new StartServiceOrderController(StartServiceOrderFactory.make(jdbi), validation);
  }
}
