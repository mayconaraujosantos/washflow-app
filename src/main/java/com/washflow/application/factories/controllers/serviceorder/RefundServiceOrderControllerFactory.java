package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.RefundServiceOrderFactory;
import com.washflow.presentation.controllers.serviceorder.RefundServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class RefundServiceOrderControllerFactory {

  private RefundServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new RefundServiceOrderController(RefundServiceOrderFactory.make(jdbi));
  }
}
