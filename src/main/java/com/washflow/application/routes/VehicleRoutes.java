package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.CreateVehicleControllerFactory;
import com.washflow.application.factories.controllers.ListVehiclesByCustomerControllerFactory;
import com.washflow.domain.entities.Vehicle;
import com.washflow.presentation.controllers.createvehicle.CreateVehicleRequest;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class VehicleRoutes {

  private static final String VEHICLES_PATH = "/api/vehicles";
  private static final String CUSTOMER_VEHICLES_PATH = "/api/customers/{id}/vehicles";

  private VehicleRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi) {
    registerCreate(jdbi);
    registerListByCustomer(jdbi);
  }

  @OpenApi(
      path = VEHICLES_PATH,
      methods = HttpMethod.POST,
      summary = "Register a vehicle for an existing customer",
      tags = {"Vehicles"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = CreateVehicleRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Vehicle.class)),
        @OpenApiResponse(status = "400", description = "Missing or malformed field"),
        @OpenApiResponse(status = "404", description = "Customer not found"),
        @OpenApiResponse(status = "409", description = "Plate already registered")
      })
  private static void registerCreate(Jdbi jdbi) {
    post(VEHICLES_PATH, JavalinRouteAdapter.adapt(CreateVehicleControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CUSTOMER_VEHICLES_PATH,
      methods = HttpMethod.GET,
      summary = "List a customer's registered vehicles",
      tags = {"Vehicles"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Vehicle[].class)),
        @OpenApiResponse(status = "400", description = "Malformed customer id"),
        @OpenApiResponse(status = "404", description = "Customer not found")
      })
  private static void registerListByCustomer(Jdbi jdbi) {
    get(
        CUSTOMER_VEHICLES_PATH,
        JavalinRouteAdapter.adapt(ListVehiclesByCustomerControllerFactory.make(jdbi)));
  }
}
