package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.ListServicePricesControllerFactory;
import com.washflow.domain.entities.ServicePrice;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class ServicePriceRoutes {

  private static final String BASE_PATH = "/api/services";

  private ServicePriceRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi) {
    registerList(jdbi);
  }

  @OpenApi(
      path = BASE_PATH,
      methods = HttpMethod.GET,
      summary = "List the wash-type catalog (RF-03)",
      tags = {"Services"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServicePrice[].class))
      })
  private static void registerList(Jdbi jdbi) {
    get(BASE_PATH, JavalinRouteAdapter.adapt(ListServicePricesControllerFactory.make(jdbi)));
  }
}
