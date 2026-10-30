package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.SaveServiceOrderPhotoFactory;
import com.washflow.presentation.controllers.savephoto.SavePhotoController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class SavePhotoControllerFactory {

  private SavePhotoControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new SavePhotoController(SaveServiceOrderPhotoFactory.make(jdbi));
  }
}
