package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.AuthenticateCustomerControllerFactory;
import com.washflow.application.factories.controllers.AuthenticateManagerControllerFactory;
import com.washflow.application.factories.controllers.AuthenticateWasherControllerFactory;
import com.washflow.application.factories.controllers.UpdateUserProfileControllerFactory;
import com.washflow.domain.entities.User;
import com.washflow.presentation.controllers.authenticateuser.AuthenticateUserRequest;
import com.washflow.presentation.controllers.updateuserprofile.UpdateUserProfileRequest;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class UserRoutes {

  private static final String USERS_BASE_PATH = "/api/users";

  private UserRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi) {
    registerAuthenticateCustomer(jdbi);
    registerAuthenticateWasher(jdbi);
    registerAuthenticateManager(jdbi);
    registerUpdateProfile(jdbi);
  }

  @OpenApi(
      path = "/api/auth/customers",
      methods = HttpMethod.POST,
      summary = "Quick phone login/registration through the CLIENTE QR code",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = AuthenticateUserRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateCustomer(Jdbi jdbi) {
    post(
        "/api/auth/customers",
        JavalinRouteAdapter.adapt(AuthenticateCustomerControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/auth/washers",
      methods = HttpMethod.POST,
      summary = "Quick phone login/registration through the LAVADOR QR code",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = AuthenticateUserRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateWasher(Jdbi jdbi) {
    post(
        "/api/auth/washers",
        JavalinRouteAdapter.adapt(AuthenticateWasherControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = "/api/auth/managers",
      methods = HttpMethod.POST,
      summary = "Login through the GERENTE QR code - never self-registers a new manager",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = AuthenticateUserRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(
            status = "403",
            description = "Phone not yet promoted to manager - see PATCH /api/users/{id}/profile"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateManager(Jdbi jdbi) {
    post(
        "/api/auth/managers",
        JavalinRouteAdapter.adapt(AuthenticateManagerControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = USERS_BASE_PATH + "/{id}/profile",
      methods = HttpMethod.PATCH,
      summary = "Manager-only: correct or promote another user's profile",
      tags = {"Users"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = UpdateUserProfileRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Missing or malformed field"),
        @OpenApiResponse(status = "403", description = "Requester is not a manager"),
        @OpenApiResponse(status = "404", description = "Requester or target user not found")
      })
  private static void registerUpdateProfile(Jdbi jdbi) {
    patch(
        USERS_BASE_PATH + "/{id}/profile",
        JavalinRouteAdapter.adapt(UpdateUserProfileControllerFactory.make(jdbi)));
  }
}
