package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.DeleteServiceOrderPhotoFactory;
import com.washflow.presentation.controllers.deletephoto.DeletePhotoController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class DeletePhotoControllerFactory {

  private DeletePhotoControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new DeletePhotoController(DeleteServiceOrderPhotoFactory.make(jdbi));
  }
}
