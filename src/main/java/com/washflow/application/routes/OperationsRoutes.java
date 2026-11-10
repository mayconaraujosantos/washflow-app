package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.finance.GetOperationsReportControllerFactory;
import com.washflow.application.factories.controllers.schedule.ListAgendaControllerFactory;
import com.washflow.application.factories.controllers.serviceorder.CancelServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.serviceorder.PauseServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.serviceorder.RefundServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.serviceorder.ResumeServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.vehicle.SearchVehiclesControllerFactory;
import com.washflow.application.factories.controllers.yard.GetWaitingQueueControllerFactory;
import com.washflow.application.factories.controllers.yard.GetWashingByWasherControllerFactory;
import com.washflow.application.factories.controllers.yard.GetYardOverviewControllerFactory;
import com.washflow.application.factories.controllers.yard.RegisterVehicleEntryControllerFactory;
import com.washflow.domain.entities.finance.OperationsReport;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderRefund;
import com.washflow.domain.entities.vehicle.VehicleLookup;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class OperationsRoutes {

  private static final String SERVICE_ORDER_BY_ID = "/api/service-orders/{id}";

  private OperationsRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    registerEntry(jdbi, appConfig);
    registerVehicleSearch(jdbi);
    registerAgenda(jdbi);
    registerPause(jdbi);
    registerResume(jdbi);
    registerCancel(jdbi);
    registerRefund(jdbi);
    registerQueue(jdbi);
    registerYard(jdbi);
    registerReport(jdbi);
    registerMyOrders(jdbi);
  }

  @OpenApi(
      path = "/api/operations/entries",
      methods = HttpMethod.POST,
      summary = "Manager registers a vehicle entry (walk-in) or schedules it, by plate",
      tags = {"Operations"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(
            status = "400",
            description = "Invalid plate, phone, dates or outside business hours"),
        @OpenApiResponse(status = "404", description = "Service not found"),
        @OpenApiResponse(status = "409", description = "Time slot fully booked")
      })
  private static void registerEntry(Jdbi jdbi, AppConfig appConfig) {
    post(
        "/api/operations/entries",
        JavalinRouteAdapter.adapt(RegisterVehicleEntryControllerFactory.make(jdbi, appConfig)));
  }

  @OpenApi(
      path = "/api/operations/vehicles",
      methods = HttpMethod.GET,
      summary = "Search vehicles by plate prefix, owner name or phone",
      tags = {"Operations"},
      queryParams = @OpenApiParam(name = "query", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = VehicleLookup[].class))
      })
  private static void registerVehicleSearch(Jdbi jdbi) {
    get(
        "/api/operations/vehicles",
        JavalinRouteAdapter.adapt(SearchVehiclesControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/operations/agenda",
      methods = HttpMethod.GET,
      summary = "Service orders scheduled between two shop-local dates (max 62 days)",
      tags = {"Operations"},
      queryParams = {
        @OpenApiParam(name = "from", type = String.class, required = true),
        @OpenApiParam(name = "to", type = String.class, required = true)
      },
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder[].class)),
        @OpenApiResponse(status = "400", description = "Invalid date range")
      })
  private static void registerAgenda(Jdbi jdbi) {
    get(
        "/api/operations/agenda",
        JavalinRouteAdapter.adapt(ListAgendaControllerFactory.make(jdbi)));
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
      path = SERVICE_ORDER_BY_ID + "/cancel",
      methods = HttpMethod.PATCH,
      summary = "Manager cancels an order that never reached washing, recording the reason",
      tags = {"Operations"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "Service order cancelled"),
        @OpenApiResponse(status = "400", description = "Invalid cancellation reason"),
        @OpenApiResponse(
            status = "409",
            description = "Service order is no longer scheduled or waiting in the yard")
      })
  private static void registerCancel(Jdbi jdbi) {
    patch(
        SERVICE_ORDER_BY_ID + "/cancel",
        JavalinRouteAdapter.adapt(CancelServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/refund",
      methods = HttpMethod.PATCH,
      summary =
          "Manager refunds the full payment of a finalized order (FINALIZADO -> ESTORNADO), dated"
              + " today; reverses the washer's commission only with reverseCommission",
      tags = {"Operations"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = ServiceOrderRefund.class)),
        @OpenApiResponse(status = "400", description = "Invalid reason or refund method"),
        @OpenApiResponse(status = "409", description = "Service order is not finalized")
      })
  private static void registerRefund(Jdbi jdbi) {
    patch(
        SERVICE_ORDER_BY_ID + "/refund",
        JavalinRouteAdapter.adapt(RefundServiceOrderControllerFactory.make(jdbi)));
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
