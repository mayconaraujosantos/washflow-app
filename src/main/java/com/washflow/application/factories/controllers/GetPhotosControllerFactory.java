package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListServiceOrderPhotosFactory;
import com.washflow.presentation.controllers.getphotos.GetPhotosController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class GetPhotosControllerFactory {

  private GetPhotosControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new GetPhotosController(ListServiceOrderPhotosFactory.make(jdbi));
  }
}
