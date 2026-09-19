package com.washflow.main.factories.controllers;

import com.washflow.main.factories.usecases.ScheduleServiceOrderFactory;
import com.washflow.presentation.controllers.scheduleserviceorder.ScheduleServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public final class ScheduleServiceOrderControllerFactory {

  private ScheduleServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("vehicleId"),
                new RequiredFieldValidation("servicePriceId"),
                new RequiredFieldValidation("scheduledAt")));

    return new ScheduleServiceOrderController(ScheduleServiceOrderFactory.make(jdbi), validation);
  }
}
