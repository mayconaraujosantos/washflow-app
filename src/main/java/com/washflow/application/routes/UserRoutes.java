package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.AuthenticateCustomerControllerFactory;
import com.washflow.application.factories.controllers.AuthenticateManagerControllerFactory;
import com.washflow.application.factories.controllers.AuthenticateWasherControllerFactory;
import com.washflow.application.factories.controllers.UpdateUserProfileControllerFactory;
import com.washflow.application.security.AuthTokenService;
import com.washflow.application.security.AuthenticatedSession;
import com.washflow.data.protocols.PasswordHasher;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserHasActiveWashesError;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.presentation.controllers.authenticateuser.AuthenticateCredentialsRequest;
import com.washflow.presentation.controllers.authenticateuser.AuthenticateUserRequest;
import com.washflow.presentation.controllers.updateuserprofile.UpdateUserProfileRequest;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public final class UserRoutes {

  private static final String USERS_BASE_PATH = "/api/users";

  private UserRoutes() {}

  /** Called from inside {@code config.routes.apiBuilder(...)} in Application. */
  public static void register(Jdbi jdbi, AppConfig appConfig) {
    AuthTokenService tokenService = new AuthTokenService(appConfig.tokenSecret());
    registerAuthenticateCustomer(jdbi, appConfig, tokenService);
    registerAuthenticateWasher(jdbi, appConfig, tokenService);
    registerAuthenticateManager(jdbi, appConfig, tokenService);
    registerUpdateProfile(jdbi);
    registerManagerUserAdministration(jdbi);
  }

  @OpenApi(
      path = "/api/auth/customers",
      methods = HttpMethod.POST,
      summary = "Register or sign in as a customer with phone and password",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = AuthenticateUserRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = AuthenticatedSession.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(status = "401", description = "Invalid phone or password"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateCustomer(
      Jdbi jdbi, AppConfig appConfig, AuthTokenService tokenService) {
    post(
        "/api/auth/customers",
        JavalinRouteAdapter.adaptAuthenticated(
            AuthenticateCustomerControllerFactory.make(jdbi, appConfig), tokenService));
  }

  @OpenApi(
      path = "/api/auth/washers",
      methods = HttpMethod.POST,
      summary = "Sign in as a washer with phone and password",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(
              content = @OpenApiContent(from = AuthenticateCredentialsRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = AuthenticatedSession.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(status = "401", description = "Invalid phone or password"),
        @OpenApiResponse(
            status = "403",
            description = "Washer account must be provisioned by a manager"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateWasher(
      Jdbi jdbi, AppConfig appConfig, AuthTokenService tokenService) {
    post(
        "/api/auth/washers",
        JavalinRouteAdapter.adaptAuthenticated(
            AuthenticateWasherControllerFactory.make(jdbi, appConfig), tokenService));
  }

  @OpenApi(
      path = "/api/auth/managers",
      methods = HttpMethod.POST,
      summary = "Sign in as a manager with phone and password",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(
              content = @OpenApiContent(from = AuthenticateCredentialsRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = AuthenticatedSession.class)),
        @OpenApiResponse(status = "400", description = "Missing field"),
        @OpenApiResponse(status = "401", description = "Invalid phone or password"),
        @OpenApiResponse(
            status = "403",
            description = "Phone not yet promoted to manager - see PATCH /api/users/{id}/profile"),
        @OpenApiResponse(
            status = "409",
            description = "Phone already registered under another profile")
      })
  private static void registerAuthenticateManager(
      Jdbi jdbi, AppConfig appConfig, AuthTokenService tokenService) {
    post(
        "/api/auth/managers",
        JavalinRouteAdapter.adaptAuthenticated(
            AuthenticateManagerControllerFactory.make(jdbi, appConfig), tokenService));
  }

  @OpenApi(
      path = USERS_BASE_PATH + "/{id}/profile",
      methods = HttpMethod.PATCH,
      summary = "Manager-only: change an account's profile",
      tags = {"Users"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = UpdateUserProfileRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Missing or malformed field"),
        @OpenApiResponse(status = "403", description = "Requester is not a manager"),
        @OpenApiResponse(status = "404", description = "Requester or target user not found"),
        @OpenApiResponse(status = "409", description = "User has active washes")
      })
  private static void registerUpdateProfile(Jdbi jdbi) {
    patch(
        USERS_BASE_PATH + "/{id}/profile",
        JavalinRouteAdapter.adapt(UpdateUserProfileControllerFactory.make(jdbi)));
  }

  private static void registerManagerUserAdministration(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    PasswordHasher passwordHasher = new BCryptPasswordHasher();
    registerListUsers(repository);
    registerCreateWasher(repository, passwordHasher);
    registerUpdateActive(repository);
  }

  @OpenApi(
      path = USERS_BASE_PATH,
      methods = HttpMethod.GET,
      summary = "Manager-only: list accounts and their active state",
      tags = {"Users"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User[].class)),
        @OpenApiResponse(status = "401", description = "Authentication required"),
        @OpenApiResponse(status = "403", description = "Requester is not a manager")
      })
  private static void registerListUsers(UserJdbiRepository repository) {
    get(USERS_BASE_PATH, ctx -> ctx.json(repository.loadAll()));
  }

  @OpenApi(
      path = USERS_BASE_PATH,
      methods = HttpMethod.POST,
      summary = "Manager-only: create a washer account with an initial password",
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = CreateWasherRequest.class)),
      responses = {
        @OpenApiResponse(status = "201", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Invalid account details"),
        @OpenApiResponse(status = "401", description = "Authentication required"),
        @OpenApiResponse(status = "403", description = "Requester is not a manager"),
        @OpenApiResponse(status = "409", description = "Phone is already registered")
      })
  private static void registerCreateWasher(
      UserJdbiRepository repository, PasswordHasher passwordHasher) {
    post(USERS_BASE_PATH, ctx -> createWasher(ctx, repository, passwordHasher));
  }

  @OpenApi(
      path = USERS_BASE_PATH + "/{id}/active",
      methods = HttpMethod.PATCH,
      summary = "Manager-only: activate or deactivate an account",
      tags = {"Users"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      requestBody =
          @OpenApiRequestBody(content = @OpenApiContent(from = UpdateUserActiveRequest.class)),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "400", description = "Invalid user id or request"),
        @OpenApiResponse(status = "401", description = "Authentication required"),
        @OpenApiResponse(status = "403", description = "Requester is not a manager"),
        @OpenApiResponse(status = "404", description = "User not found"),
        @OpenApiResponse(status = "409", description = "Washer has active orders")
      })
  private static void registerUpdateActive(UserJdbiRepository repository) {
    patch(USERS_BASE_PATH + "/{id}/active", ctx -> updateActive(ctx, repository));
  }

  private static void createWasher(
      Context ctx, UserJdbiRepository repository, PasswordHasher passwordHasher) {
    CreateWasherRequest request;
    try {
      request = ctx.bodyAsClass(CreateWasherRequest.class);
    } catch (RuntimeException e) {
      ctx.status(400).json(Map.of("error", "Provide name, phone, and an initial password"));
      return;
    }
    if (request == null) {
      ctx.status(400).json(Map.of("error", "Provide name, phone, and an initial password"));
      return;
    }

    String name = request.name() == null ? "" : request.name().trim();
    String phone = request.phone() == null ? "" : request.phone().trim();
    String password = request.password() == null ? "" : request.password();
    if (name.isBlank() || name.length() > 100 || phone.isBlank() || phone.length() > 20) {
      ctx.status(400).json(Map.of("error", "Name and phone are required and must be valid"));
      return;
    }
    if (password.length() < 8) {
      ctx.status(400).json(Map.of("error", "Password must contain at least eight characters"));
      return;
    }
    if (repository.loadByPhone(phone).isPresent()) {
      ctx.status(409).json(Map.of("error", "Phone is already registered"));
      return;
    }

    User washer = new User(UUID.randomUUID(), name, phone, UserProfile.WASHER, Instant.now(), true);
    try {
      ctx.status(201).json(repository.create(washer, passwordHasher.hash(password)));
    } catch (UnableToExecuteStatementException e) {
      if (hasUniqueConstraintViolation(e)) {
        ctx.status(409).json(Map.of("error", "Phone is already registered"));
        return;
      }
      throw e;
    }
  }

  private static void updateActive(Context ctx, UserJdbiRepository repository) {
    UUID userId;
    try {
      userId = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Invalid user id"));
      return;
    }

    UpdateUserActiveRequest request;
    try {
      request = ctx.bodyAsClass(UpdateUserActiveRequest.class);
    } catch (RuntimeException e) {
      ctx.status(400).json(Map.of("error", "Provide the active field"));
      return;
    }
    if (request == null || request.active() == null) {
      ctx.status(400).json(Map.of("error", "Provide the active field"));
      return;
    }

    User user = repository.loadById(userId).orElse(null);
    if (user == null) {
      ctx.status(404).json(Map.of("error", "User not found"));
      return;
    }
    try {
      if (!repository.updateActive(userId, request.active())) {
        ctx.status(404).json(Map.of("error", "User not found"));
        return;
      }
    } catch (UserHasActiveWashesError e) {
      ctx.status(409)
          .json(Map.of("error", "Complete or reassign this washer's active orders first"));
      return;
    }
    ctx.json(repository.loadById(userId).orElseThrow());
  }

  private static boolean hasUniqueConstraintViolation(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof java.sql.SQLException sqlException
          && "23505".equals(sqlException.getSQLState())) {
        return true;
      }
    }
    return false;
  }

  public record CreateWasherRequest(String name, String phone, String password) {}

  public record UpdateUserActiveRequest(Boolean active) {}
}
