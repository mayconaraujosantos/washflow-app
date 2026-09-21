package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.CheckInServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.CompleteServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.FinalizeServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.GetServiceOrderByIdControllerFactory;
import com.washflow.application.factories.controllers.ListServiceOrdersByCustomerControllerFactory;
import com.washflow.application.factories.controllers.ScheduleServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.StartServiceOrderControllerFactory;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.presentation.controllers.scheduleserviceorder.ScheduleServiceOrderRequest;
import com.washflow.presentation.controllers.startserviceorder.StartServiceOrderRequest;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class ServiceOrderRoutes {

  private static final String BASE_PATH = "/api/service-orders";
  private static final String BY_ID_PATH = BASE_PATH + "/{id}";
  private static final String CUSTOMER_SERVICE_ORDERS_PATH = "/api/customers/{id}/service-orders";

  private ServiceOrderRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi) {
    registerSchedule(jdbi);
    registerCheckIn(jdbi);
    registerStart(jdbi);
    registerComplete(jdbi);
    registerFinalize(jdbi);
    registerGetById(jdbi);
    registerListByCustomer(jdbi);
  }

  @OpenApi(
      path = BASE_PATH,
      methods = HttpMethod.POST,
      summary = "Schedule a new service order",
      tags = {"Service Orders"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = ScheduleServiceOrderRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(
            status = "400",
            description =
                "Missing/malformed field, outside business hours (Mon-Sat 08:00-18:00), or"
                    + " under 30min lead time"),
        @OpenApiResponse(status = "404", description = "Vehicle or service price not found"),
        @OpenApiResponse(status = "409", description = "Time slot fully booked")
      })
  private static void registerSchedule(Jdbi jdbi) {
    post(BASE_PATH, JavalinRouteAdapter.adapt(ScheduleServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BY_ID_PATH,
      methods = HttpMethod.PATCH,
      summary = "Check a vehicle into the yard (AGENDADO -> AGUARDANDO_PATIO)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id"),
        @OpenApiResponse(status = "404", description = "Service order not found"),
        @OpenApiResponse(status = "409", description = "Service order not in AGENDADO status")
      })
  private static void registerCheckIn(Jdbi jdbi) {
    patch(
        BY_ID_PATH + "/check-in",
        JavalinRouteAdapter.adapt(CheckInServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BY_ID_PATH,
      methods = HttpMethod.PATCH,
      summary = "Assign a washer and start washing (AGUARDANDO_PATIO -> EM_LAVAGEM)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = StartServiceOrderRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Missing or malformed field"),
        @OpenApiResponse(status = "404", description = "Service order not found"),
        @OpenApiResponse(
            status = "409",
            description = "Service order not in AGUARDANDO_PATIO status")
      })
  private static void registerStart(Jdbi jdbi) {
    patch(
        BY_ID_PATH + "/start",
        JavalinRouteAdapter.adapt(StartServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BY_ID_PATH,
      methods = HttpMethod.PATCH,
      summary = "Finish washing, vehicle ready for pickup (EM_LAVAGEM -> PRONTO)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id"),
        @OpenApiResponse(status = "404", description = "Service order not found"),
        @OpenApiResponse(status = "409", description = "Service order not in EM_LAVAGEM status")
      })
  private static void registerComplete(Jdbi jdbi) {
    patch(
        BY_ID_PATH + "/complete",
        JavalinRouteAdapter.adapt(CompleteServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BY_ID_PATH,
      methods = HttpMethod.PATCH,
      summary = "Close out the sale and credit the washer's commission (PRONTO -> FINALIZADO)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id"),
        @OpenApiResponse(status = "404", description = "Service order or service price not found"),
        @OpenApiResponse(status = "409", description = "Service order not in PRONTO status")
      })
  private static void registerFinalize(Jdbi jdbi) {
    patch(
        BY_ID_PATH + "/finalize",
        JavalinRouteAdapter.adapt(FinalizeServiceOrderControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BY_ID_PATH,
      methods = HttpMethod.GET,
      summary = "Track a service order's current status (RF-06)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder.class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id"),
        @OpenApiResponse(status = "404", description = "Service order not found")
      })
  private static void registerGetById(Jdbi jdbi) {
    get(BY_ID_PATH, JavalinRouteAdapter.adapt(GetServiceOrderByIdControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CUSTOMER_SERVICE_ORDERS_PATH,
      methods = HttpMethod.GET,
      summary = "List a customer's service orders, most recently updated first (RF-06)",
      tags = {"Service Orders"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServiceOrder[].class)),
        @OpenApiResponse(status = "400", description = "Malformed customer id"),
        @OpenApiResponse(status = "404", description = "Customer not found")
      })
  private static void registerListByCustomer(Jdbi jdbi) {
    get(
        CUSTOMER_SERVICE_ORDERS_PATH,
        JavalinRouteAdapter.adapt(ListServiceOrdersByCustomerControllerFactory.make(jdbi)));
  }
}
