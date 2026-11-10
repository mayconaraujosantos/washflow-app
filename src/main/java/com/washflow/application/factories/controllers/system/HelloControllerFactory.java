package com.washflow.application.factories.controllers.system;

import com.washflow.presentation.controllers.system.HelloController;
import com.washflow.presentation.protocols.Controller;

public final class HelloControllerFactory {

  private HelloControllerFactory() {}

  public static Controller make() {
    return new HelloController();
  }
}
