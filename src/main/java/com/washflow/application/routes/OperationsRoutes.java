package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.GetOperationsReportControllerFactory;
import com.washflow.application.factories.controllers.GetWaitingQueueControllerFactory;
import com.washflow.application.factories.controllers.GetWashingByWasherControllerFactory;
import com.washflow.application.factories.controllers.GetYardOverviewControllerFactory;
import com.washflow.application.factories.controllers.PauseServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.ResumeServiceOrderControllerFactory;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.infra.db.jdbi.OperationsReport;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class OperationsRoutes {

  private static final String SERVICE_ORDER_BY_ID = "/api/service-orders/{id}";

  private OperationsRoutes() {}

  public static void register(Jdbi jdbi) {
    registerPause(jdbi);
    registerResume(jdbi);
    registerQueue(jdbi);
    registerYard(jdbi);
    registerReport(jdbi);
    registerMyOrders(jdbi);
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/pause",
      methods = HttpMethod.PATCH,
      summary = "Pause an active wash, recording the reason",
      tags = {"Operations"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "Wash paused"),
        @OpenApiResponse(status = "400", description = "Invalid pause reason"),
        @OpenApiResponse(
            status = "409",
            description = "Service order is not an active wash assigned to you")
      })
  private static void registerPause(Jdbi jdbi) {
    patch(
        SERVICE_ORDER_BY_ID + "/pause",
        JavalinRouteAdapter.adapt(PauseServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/resume",
      methods = HttpMethod.PATCH,
      summary = "Resume a paused wash",
      tags = {"Operations"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "Wash resumed"),
        @OpenApiResponse(
            status = "409",
            description = "Service order is not paused or is not assigned to you")
      })
  private static void registerResume(Jdbi jdbi) {
    patch(
        SERVICE_ORDER_BY_ID + "/resume",
        JavalinRouteAdapter.adapt(ResumeServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/operations/queue",
      methods = HttpMethod.GET,
      summary = "Get the waiting queue of service orders",
      tags = {"Operations"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder[].class))
      })
  private static void registerQueue(Jdbi jdbi) {
    get(
        "/api/operations/queue",
        JavalinRouteAdapter.adapt(GetWaitingQueueControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/operations/yard",
      methods = HttpMethod.GET,
      summary = "Get the yard overview of service orders",
      tags = {"Operations"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder[].class))
      })
  private static void registerYard(Jdbi jdbi) {
    get(
        "/api/operations/yard",
        JavalinRouteAdapter.adapt(GetYardOverviewControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/operations/report",
      methods = HttpMethod.GET,
      summary = "Get an operations report for a date range",
      tags = {"Operations"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = OperationsReport.class)),
        @OpenApiResponse(status = "400", description = "Invalid date range")
      })
  private static void registerReport(Jdbi jdbi) {
    get(
        "/api/operations/report",
        JavalinRouteAdapter.adapt(GetOperationsReportControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/operations/my-orders",
      methods = HttpMethod.GET,
      summary = "Get the active service orders assigned to the current washer",
      tags = {"Operations"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder[].class))
      })
  private static void registerMyOrders(Jdbi jdbi) {
    get(
        "/api/operations/my-orders",
        JavalinRouteAdapter.adapt(GetWashingByWasherControllerFactory.make(jdbi)));
  }
}
