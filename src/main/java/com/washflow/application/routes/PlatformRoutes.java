package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.usecases.EstablishmentFactory;
import com.washflow.application.security.AuthTokenService;
import com.washflow.domain.entities.EstablishmentListing;
import com.washflow.domain.entities.EstablishmentSummary;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.controllers.platform.PlatformController;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

/** Platform administration (lava-jatos) and the public directory. */
public final class PlatformRoutes {

  private static final String ADMIN_ESTABLISHMENTS = "/api/admin/establishments";

  private PlatformRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    ManagePlatform platform = EstablishmentFactory.platform(jdbi);
    registerSignIn(platform, appConfig);
    registerDirectory(platform);
    registerList(platform);
    registerCreate(platform);
    registerSetActive(platform);
  }

  @OpenApi(
      path = "/api/auth/admins",
      methods = HttpMethod.POST,
      summary = "Platform administrator sign-in (email + password)",
      tags = {"Platform"},
      responses = {
        @OpenApiResponse(status = "200", description = "Admin session"),
        @OpenApiResponse(status = "401", description = "Invalid credentials")
      })
  private static void registerSignIn(ManagePlatform platform, AppConfig appConfig) {
    post(
        "/api/auth/admins",
        JavalinRouteAdapter.adaptAuthenticated(
            PlatformController.signIn(platform), new AuthTokenService(appConfig.tokenSecret())));
  }

  @OpenApi(
      path = "/api/establishments",
      methods = HttpMethod.GET,
      summary = "Active lava-jatos matching a name, for the customer picker (public)",
      tags = {"Platform"},
      queryParams = @OpenApiParam(name = "query", required = false),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = EstablishmentListing[].class))
      })
  private static void registerDirectory(ManagePlatform platform) {
    get("/api/establishments", JavalinRouteAdapter.adapt(PlatformController.search(platform)));
  }

  @OpenApi(
      path = ADMIN_ESTABLISHMENTS,
      methods = HttpMethod.GET,
      summary = "All lava-jatos (platform administrator)",
      tags = {"Platform"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = EstablishmentSummary[].class))
      })
  private static void registerList(ManagePlatform platform) {
    get(ADMIN_ESTABLISHMENTS, JavalinRouteAdapter.adapt(PlatformController.list(platform)));
  }

  @OpenApi(
      path = ADMIN_ESTABLISHMENTS,
      methods = HttpMethod.POST,
      summary = "Create a lava-jato with its first manager and a starter catalog (admin)",
      tags = {"Platform"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = EstablishmentSummary.class)),
        @OpenApiResponse(status = "400", description = "Invalid data"),
        @OpenApiResponse(status = "409", description = "Address or e-mail already in use")
      })
  private static void registerCreate(ManagePlatform platform) {
    post(ADMIN_ESTABLISHMENTS, JavalinRouteAdapter.adapt(PlatformController.create(platform)));
  }

  @OpenApi(
      path = ADMIN_ESTABLISHMENTS + "/{id}/active",
      methods = HttpMethod.PATCH,
      summary = "Suspend or reactivate a lava-jato (admin)",
      tags = {"Platform"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = EstablishmentSummary.class)),
        @OpenApiResponse(status = "404", description = "Unknown lava-jato")
      })
  private static void registerSetActive(ManagePlatform platform) {
    patch(
        ADMIN_ESTABLISHMENTS + "/{id}/active",
        JavalinRouteAdapter.adapt(PlatformController.setActive(platform)));
  }
}
