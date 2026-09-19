package com.washflow.main.routes;

import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.main.adapters.JavalinRouteAdapter;
import com.washflow.main.factories.controllers.AgendarAtendimentoControllerFactory;
import org.jdbi.v3.core.Jdbi;

public final class AtendimentoRoutes {

  private AtendimentoRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Main. */
  public static void register(Jdbi jdbi) {
    post(
        "/api/atendimentos",
        JavalinRouteAdapter.adapt(AgendarAtendimentoControllerFactory.make(jdbi)));
  }
}
