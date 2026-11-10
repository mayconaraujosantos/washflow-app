package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.RefundServiceOrderFactory;
import com.washflow.presentation.controllers.refundserviceorder.RefundServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class RefundServiceOrderControllerFactory {

  private RefundServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new RefundServiceOrderController(RefundServiceOrderFactory.make(jdbi));
  }
}
