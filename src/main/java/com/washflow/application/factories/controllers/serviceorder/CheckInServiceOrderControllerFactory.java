package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.CheckInServiceOrderFactory;
import com.washflow.presentation.controllers.serviceorder.CheckInServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CheckInServiceOrderControllerFactory {

  private CheckInServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CheckInServiceOrderController(CheckInServiceOrderFactory.make(jdbi));
  }
}
