package com.washflow.application.modules;

import com.washflow.infra.db.jdbi.DatabaseHealthJdbiRepository;
import com.washflow.presentation.controllers.system.HealthController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the health check endpoint. */
public final class SystemModule {

  private SystemModule() {}

  public static Controller healthController(Jdbi jdbi) {
    return new HealthController(new DatabaseHealthJdbiRepository(jdbi));
  }
}
