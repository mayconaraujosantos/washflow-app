package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.CreateSaleControllerFactory;
import com.washflow.application.factories.controllers.ListSalesControllerFactory;
import com.washflow.domain.entities.Sale;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

/** Quick counter sales and budgets (manager only, see RoutePermissionMatcher). */
public final class SaleRoutes {

  private static final String BASE_PATH = "/api/sales";

  private SaleRoutes() {}

  public static void register(Jdbi jdbi) {
    registerCreate(jdbi);
    registerList(jdbi);
  }

  @OpenApi(
      path = BASE_PATH,
      methods = HttpMethod.POST,
      summary = "Register a quick sale or a budget with catalog items and payments",
      tags = {"Sales"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Sale.class)),
        @OpenApiResponse(status = "400", description = "Invalid items, discount or payments"),
        @OpenApiResponse(status = "404", description = "Service or vehicle not found")
      })
  private static void registerCreate(Jdbi jdbi) {
    post(BASE_PATH, JavalinRouteAdapter.adapt(CreateSaleControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = BASE_PATH,
      methods = HttpMethod.GET,
      summary = "List sales and budgets created between two shop-local dates",
      tags = {"Sales"},
      queryParams = {
        @OpenApiParam(name = "from", type = String.class, required = true),
        @OpenApiParam(name = "to", type = String.class, required = true),
        @OpenApiParam(name = "type", type = String.class)
      },
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Sale[].class)),
        @OpenApiResponse(status = "400", description = "Invalid range or type")
      })
  private static void registerList(Jdbi jdbi) {
    get(BASE_PATH, JavalinRouteAdapter.adapt(ListSalesControllerFactory.make(jdbi)));
  }
}
