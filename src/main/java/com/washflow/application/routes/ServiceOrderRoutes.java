package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.ScheduleServiceOrderControllerFactory;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.presentation.controllers.scheduleserviceorder.ScheduleServiceOrderRequest;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class ServiceOrderRoutes {

  private ServiceOrderRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  @OpenApi(
      path = "/api/service-orders",
      methods = HttpMethod.POST,
      summary = "Schedule a new service order",
      tags = {"Service Orders"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = ScheduleServiceOrderRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Missing or malformed field"),
        @OpenApiResponse(status = "404", description = "Vehicle or service price not found")
      })
  public static void register(Jdbi jdbi) {
    post(
        "/api/service-orders",
        JavalinRouteAdapter.adapt(ScheduleServiceOrderControllerFactory.make(jdbi)));
  }
}
