package com.washflow.application.modules;

import com.washflow.infra.db.jdbi.DatabaseHealthJdbiRepository;
import com.washflow.presentation.controllers.system.HealthController;
import com.washflow.presentation.controllers.system.HelloController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

/** Composition root of health and hello endpoints: builds its use cases and controllers. */
public final class SystemModule {

  private SystemModule() {}

  public static Controller healthController(Jdbi jdbi) {
    return new HealthController(new DatabaseHealthJdbiRepository(jdbi));
  }

  public static Controller helloController() {
    return new HelloController();
  }
}
