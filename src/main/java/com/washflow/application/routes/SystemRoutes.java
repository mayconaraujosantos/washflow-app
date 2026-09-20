package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.HealthControllerFactory;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class SystemRoutes {

  private SystemRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi) {
    registerHealth(jdbi);
  }

  @OpenApi(
      path = "/api/health",
      methods = HttpMethod.GET,
      summary = "Liveness and database connectivity check",
      responses = @OpenApiResponse(status = "200"))
  private static void registerHealth(Jdbi jdbi) {
    get("/api/health", JavalinRouteAdapter.adapt(HealthControllerFactory.make(jdbi)));
  }
}
