package com.washflow.application.factories.controllers.serviceorder;

import com.washflow.application.factories.usecases.serviceorder.ListServiceOrderPhotosFactory;
import com.washflow.presentation.controllers.serviceorder.GetPhotosController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetPhotosControllerFactory {

  private GetPhotosControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetPhotosController(ListServiceOrderPhotosFactory.make(jdbi));
  }
}
