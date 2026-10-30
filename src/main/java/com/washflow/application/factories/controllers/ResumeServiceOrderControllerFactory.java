package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ResumeServiceOrderFactory;
import com.washflow.presentation.controllers.resumeserviceorder.ResumeServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ResumeServiceOrderControllerFactory {

  private ResumeServiceOrderControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ResumeServiceOrderController(ResumeServiceOrderFactory.make(jdbi));
  }
}
