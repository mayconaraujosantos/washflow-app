package com.washflow.application.factories.controllers.yard;

import com.washflow.application.factories.usecases.yard.GetWaitingQueueFactory;
import com.washflow.presentation.controllers.getwaitingqueue.GetWaitingQueueController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetWaitingQueueControllerFactory {

  private GetWaitingQueueControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetWaitingQueueController(GetWaitingQueueFactory.make(jdbi));
  }
}
