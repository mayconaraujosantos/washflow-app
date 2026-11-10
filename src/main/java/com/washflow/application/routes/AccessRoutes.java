package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.usecases.access.AccessFactory;
import com.washflow.application.security.AuthTokenService;
import com.washflow.domain.entities.TicketView;
import com.washflow.domain.usecases.access.WasherBadge;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.presentation.controllers.access.AccessControllers;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

/** Washer badge/PIN, public order tickets and passwordless customer login. */
public final class AccessRoutes {

  private static final String USER_BADGE = "/api/users/{id}/badge";
  private static final String TICKET_ISSUE = "/api/service-orders/{id}/ticket";
  private static final String TICKET_VIEW = "/api/tickets/{token}";

  private AccessRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    WasherBadge badge = AccessFactory.washerBadge(jdbi, appConfig);
    ServiceOrderTicket ticket = AccessFactory.ticket(jdbi);
    CustomerPasswordlessLogin customer = AccessFactory.customerLogin(jdbi, appConfig);
    AuthTokenService tokens = new AuthTokenService(appConfig.tokenSecret());
    registerIssueBadge(badge);
    registerPinLogin(badge, tokens);
    registerBadgeLogin(badge, tokens);
    registerIssueTicket(ticket);
    registerViewTicket(ticket);
    registerOptions(customer);
    registerRequestCode(customer);
    registerVerifyCode(customer, tokens);
    registerGoogle(customer, tokens);
  }

  @OpenApi(
      path = USER_BADGE,
      methods = HttpMethod.POST,
      summary = "Issue a washer badge: new PIN and QR token, shown once (manager)",
      tags = {"Access"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = WasherBadge.Issued.class)),
        @OpenApiResponse(status = "400", description = "Not a washer")
      })
  private static void registerIssueBadge(WasherBadge badge) {
    post(USER_BADGE, JavalinRouteAdapter.adapt(AccessControllers.issueBadge(badge)));
  }

  @OpenApi(
      path = "/api/auth/washers/pin",
      methods = HttpMethod.POST,
      summary = "Washer login with phone + 6-digit PIN (locks after 5 failures)",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "Session"),
        @OpenApiResponse(status = "401", description = "Invalid credentials"),
        @OpenApiResponse(status = "429", description = "Temporarily locked")
      })
  private static void registerPinLogin(WasherBadge badge, AuthTokenService tokens) {
    post(
        "/api/auth/washers/pin",
        JavalinRouteAdapter.adaptAuthenticated(AccessControllers.pinLogin(badge), tokens));
  }

  @OpenApi(
      path = "/api/auth/washers/badge",
      methods = HttpMethod.POST,
      summary = "Washer login by scanning the badge QR",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "Session"),
        @OpenApiResponse(status = "401", description = "Unknown or revoked badge")
      })
  private static void registerBadgeLogin(WasherBadge badge, AuthTokenService tokens) {
    post(
        "/api/auth/washers/badge",
        JavalinRouteAdapter.adaptAuthenticated(AccessControllers.badgeLogin(badge), tokens));
  }

  @OpenApi(
      path = TICKET_ISSUE,
      methods = HttpMethod.POST,
      summary = "Get (or create) the order's public ticket token (manager)",
      tags = {"Access"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "{token}"),
        @OpenApiResponse(status = "404", description = "Order not found")
      })
  private static void registerIssueTicket(ServiceOrderTicket ticket) {
    post(TICKET_ISSUE, JavalinRouteAdapter.adapt(AccessControllers.issueTicket(ticket)));
  }

  @OpenApi(
      path = TICKET_VIEW,
      methods = HttpMethod.GET,
      summary = "Public ticket: vehicle, service and status, no login",
      tags = {"Access"},
      pathParams = @OpenApiParam(name = "token", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = TicketView.class)),
        @OpenApiResponse(status = "404", description = "Unknown ticket")
      })
  private static void registerViewTicket(ServiceOrderTicket ticket) {
    get(TICKET_VIEW, JavalinRouteAdapter.adapt(AccessControllers.viewTicket(ticket)));
  }

  @OpenApi(
      path = "/api/auth/options",
      methods = HttpMethod.GET,
      summary = "Which passwordless customer logins are enabled (public)",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = CustomerPasswordlessLogin.Options.class))
      })
  private static void registerOptions(CustomerPasswordlessLogin customer) {
    get("/api/auth/options", JavalinRouteAdapter.adapt(AccessControllers.options(customer)));
  }

  @OpenApi(
      path = "/api/auth/customers/otp",
      methods = HttpMethod.POST,
      summary = "Send a one-time code (and magic link) to the customer's WhatsApp",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "{expiresInSeconds}"),
        @OpenApiResponse(status = "429", description = "Asked again too soon"),
        @OpenApiResponse(status = "503", description = "No WhatsApp provider configured")
      })
  private static void registerRequestCode(CustomerPasswordlessLogin customer) {
    post(
        "/api/auth/customers/otp",
        JavalinRouteAdapter.adapt(AccessControllers.requestCode(customer)));
  }

  @OpenApi(
      path = "/api/auth/customers/otp/verify",
      methods = HttpMethod.POST,
      summary = "Sign in (or sign up, with name) with the WhatsApp code",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "Session"),
        @OpenApiResponse(status = "400", description = "New phone: name required"),
        @OpenApiResponse(status = "401", description = "Wrong or expired code")
      })
  private static void registerVerifyCode(
      CustomerPasswordlessLogin customer, AuthTokenService tokens) {
    post(
        "/api/auth/customers/otp/verify",
        JavalinRouteAdapter.adaptAuthenticated(AccessControllers.verifyCode(customer), tokens));
  }

  @OpenApi(
      path = "/api/auth/customers/google",
      methods = HttpMethod.POST,
      summary = "Customer sign in with a Google ID token",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "Session"),
        @OpenApiResponse(status = "401", description = "Invalid Google credential"),
        @OpenApiResponse(status = "503", description = "Google login not configured")
      })
  private static void registerGoogle(CustomerPasswordlessLogin customer, AuthTokenService tokens) {
    post(
        "/api/auth/customers/google",
        JavalinRouteAdapter.adaptAuthenticated(AccessControllers.google(customer), tokens));
  }
}
