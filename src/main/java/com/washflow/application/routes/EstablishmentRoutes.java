package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.usecases.establishment.EstablishmentFactory;
import com.washflow.application.security.AuthTokenService;
import com.washflow.data.usecases.user.DbUpdateUserEmail;
import com.washflow.domain.entities.establishment.Establishment;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.presentation.controllers.establishment.EstablishmentController;
import com.washflow.presentation.controllers.establishment.SetupEstablishmentController;
import com.washflow.presentation.controllers.establishment.SetupStatusController;
import com.washflow.presentation.controllers.user.UpdateUserEmailController;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

/** First-run setup, establishment settings and managers' login e-mail. */
public final class EstablishmentRoutes {

  private static final String SETUP = "/api/setup";
  private static final String ESTABLISHMENT = "/api/establishment";

  private EstablishmentRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    registerSetupStatus(jdbi, appConfig);
    registerSetup(jdbi, appConfig);
    registerGetEstablishment(jdbi);
    registerUpdateEstablishment(jdbi);
    registerUpdateEmail(jdbi);
  }

  @OpenApi(
      path = SETUP,
      methods = HttpMethod.GET,
      summary = "Whether the first-run setup is still pending (public)",
      tags = {"Establishment"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = SetupEstablishment.Status.class))
      })
  private static void registerSetupStatus(Jdbi jdbi, AppConfig appConfig) {
    get(
        SETUP,
        JavalinRouteAdapter.adapt(
            new SetupStatusController(EstablishmentFactory.setup(jdbi, appConfig))));
  }

  @OpenApi(
      path = SETUP,
      methods = HttpMethod.POST,
      summary = "Create the establishment and its owner manager (once; SETUP_CODE in production)",
      tags = {"Establishment"},
      responses = {
        @OpenApiResponse(status = "200", description = "Owner session"),
        @OpenApiResponse(status = "400", description = "Invalid data"),
        @OpenApiResponse(status = "403", description = "Missing or wrong setup code"),
        @OpenApiResponse(status = "409", description = "Already configured or e-mail/phone taken")
      })
  private static void registerSetup(Jdbi jdbi, AppConfig appConfig) {
    post(
        SETUP,
        JavalinRouteAdapter.adaptAuthenticated(
            new SetupEstablishmentController(EstablishmentFactory.setup(jdbi, appConfig)),
            new AuthTokenService(appConfig.tokenSecret())));
  }

  @OpenApi(
      path = ESTABLISHMENT,
      methods = HttpMethod.GET,
      summary = "Establishment name, opening hours and yard capacity (public)",
      tags = {"Establishment"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Establishment.class))
      })
  private static void registerGetEstablishment(Jdbi jdbi) {
    get(
        ESTABLISHMENT,
        JavalinRouteAdapter.adapt(EstablishmentController.get(EstablishmentFactory.manage(jdbi))));
  }

  @OpenApi(
      path = ESTABLISHMENT,
      methods = HttpMethod.PUT,
      summary = "Update the establishment parameters (manager)",
      tags = {"Establishment"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Establishment.class)),
        @OpenApiResponse(status = "400", description = "Invalid hours or capacity")
      })
  private static void registerUpdateEstablishment(Jdbi jdbi) {
    put(
        ESTABLISHMENT,
        JavalinRouteAdapter.adapt(
            EstablishmentController.update(EstablishmentFactory.manage(jdbi))));
  }

  @OpenApi(
      path = "/api/users/{id}/email",
      methods = HttpMethod.PATCH,
      summary = "Set or clear an account's login e-mail (manager)",
      tags = {"Users"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "409", description = "E-mail already in use")
      })
  private static void registerUpdateEmail(Jdbi jdbi) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    patch(
        "/api/users/{id}/email",
        JavalinRouteAdapter.adapt(
            new UpdateUserEmailController(new DbUpdateUserEmail(users, users, users))));
  }
}
