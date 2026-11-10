package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.modules.EstablishmentHours;
import com.washflow.data.usecases.schedule.DbScheduleAvailability;
import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.presentation.controllers.schedule.ScheduleAvailabilityController;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class ScheduleRoutes {

  private ScheduleRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    registerAvailability(jdbi, appConfig);
  }

  @OpenApi(
      path = "/api/schedule/availability",
      methods = HttpMethod.GET,
      summary = "Bookable hours of a shop-local day with remaining capacity (public)",
      tags = {"Schedule"},
      queryParams = @OpenApiParam(name = "date", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = ScheduleAvailability.Day.class)),
        @OpenApiResponse(status = "400", description = "Invalid or out-of-range date")
      })
  private static void registerAvailability(Jdbi jdbi, AppConfig appConfig) {
    get(
        "/api/schedule/availability",
        JavalinRouteAdapter.adapt(
            new ScheduleAvailabilityController(
                new DbScheduleAvailability(
                    EstablishmentHours.supplier(jdbi, appConfig),
                    new AgendaJdbiRepository(jdbi),
                    Clock.systemUTC()))));
  }
}
