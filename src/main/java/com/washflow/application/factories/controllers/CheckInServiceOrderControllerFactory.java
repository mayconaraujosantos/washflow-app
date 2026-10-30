package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CheckInServiceOrderFactory;
import com.washflow.presentation.controllers.checkinserviceorder.CheckInServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CheckInServiceOrderControllerFactory {

  private CheckInServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CheckInServiceOrderController(CheckInServiceOrderFactory.make(jdbi));
  }
}
