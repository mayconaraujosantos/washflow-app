package com.washflow.application.factories.controllers;

import com.washflow.infra.db.jdbi.DatabaseHealthJdbiRepository;
import com.washflow.presentation.controllers.health.HealthController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class HealthControllerFactory {

  private HealthControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new HealthController(new DatabaseHealthJdbiRepository(jdbi));
  }
}
