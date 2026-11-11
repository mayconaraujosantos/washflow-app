package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.modules.CustomerModule;
import com.washflow.application.modules.UserModule;
import com.washflow.application.security.AuthTokenService;
import com.washflow.domain.entities.serviceorder.TicketView;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.customer.CustomerRecoveryEmail;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.controllers.customer.CustomerLoginOptionsController;
import com.washflow.presentation.controllers.customer.CustomerRecoveryEmailController;
import com.washflow.presentation.controllers.customer.GoogleCustomerLoginController;
import com.washflow.presentation.controllers.customer.IssueServiceOrderTicketController;
import com.washflow.presentation.controllers.customer.RequestCustomerCodeController;
import com.washflow.presentation.controllers.customer.VerifyCustomerCodeController;
import com.washflow.presentation.controllers.customer.ViewServiceOrderTicketController;
import com.washflow.presentation.controllers.user.IssueWasherBadgeController;
import com.washflow.presentation.controllers.user.PasswordResetController;
import com.washflow.presentation.controllers.user.PasswordResetController.Who;
import com.washflow.presentation.controllers.user.WasherBadgeLoginController;
import com.washflow.presentation.controllers.user.WasherPinLoginController;
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
    WasherBadge badge = CustomerModule.washerBadge(jdbi, appConfig);
    ServiceOrderTicket ticket = CustomerModule.ticket(jdbi);
    CustomerPasswordlessLogin customer = CustomerModule.customerLogin(jdbi, appConfig);
    ResetPasswordByEmail passwordReset = UserModule.resetPasswordByEmail(jdbi, appConfig);
    AuthTokenService tokens = new AuthTokenService(appConfig.tokenSecret());
    registerIssueBadge(badge);
    registerPinLogin(badge, tokens);
    registerBadgeLogin(badge, tokens);
    registerIssueTicket(ticket);
    registerViewTicket(ticket);
    registerOptions(customer, passwordReset);
    registerPasswordReset(passwordReset);
    registerRecoveryEmail(CustomerModule.recoveryEmail(jdbi));
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
    post(USER_BADGE, JavalinRouteAdapter.adapt(new IssueWasherBadgeController(badge)));
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
        JavalinRouteAdapter.adaptAuthenticated(new WasherPinLoginController(badge), tokens));
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
        JavalinRouteAdapter.adaptAuthenticated(new WasherBadgeLoginController(badge), tokens));
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
    post(TICKET_ISSUE, JavalinRouteAdapter.adapt(new IssueServiceOrderTicketController(ticket)));
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
    get(TICKET_VIEW, JavalinRouteAdapter.adapt(new ViewServiceOrderTicketController(ticket)));
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
  private static void registerOptions(
      CustomerPasswordlessLogin customer, ResetPasswordByEmail passwordReset) {
    get(
        "/api/auth/options",
        JavalinRouteAdapter.adapt(new CustomerLoginOptionsController(customer, passwordReset)));
  }

  @OpenApi(
      path = "/api/auth/customers/otp",
      methods = HttpMethod.POST,
      summary = "Send a one-time code (and magic link) to the customer's e-mail or WhatsApp",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "{expiresInSeconds}"),
        @OpenApiResponse(status = "429", description = "Asked again too soon"),
        @OpenApiResponse(
            status = "503",
            description = "Channel not configured or e-mail delivery failed")
      })
  private static void registerRequestCode(CustomerPasswordlessLogin customer) {
    post(
        "/api/auth/customers/otp",
        JavalinRouteAdapter.adapt(new RequestCustomerCodeController(customer)));
  }

  @OpenApi(
      path = "/api/auth/customers/otp/verify",
      methods = HttpMethod.POST,
      summary = "Sign in (or sign up, with name) with the e-mail or WhatsApp code",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "200", description = "Session"),
        @OpenApiResponse(status = "400", description = "New account: name required"),
        @OpenApiResponse(status = "401", description = "Wrong or expired code")
      })
  private static void registerVerifyCode(
      CustomerPasswordlessLogin customer, AuthTokenService tokens) {
    post(
        "/api/auth/customers/otp/verify",
        JavalinRouteAdapter.adaptAuthenticated(new VerifyCustomerCodeController(customer), tokens));
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
        JavalinRouteAdapter.adaptAuthenticated(
            new GoogleCustomerLoginController(customer), tokens));
  }

  @OpenApi(
      path = "/api/auth/customers/password-reset",
      methods = HttpMethod.POST,
      summary =
          "Forgot password: e-mail a 6-digit code to the customer's recovery e-mail (by phone,"
              + " inside the lava-jato); same answer whether the account exists",
      tags = {"Access"},
      responses = {
        @OpenApiResponse(status = "202", description = "{expiresInSeconds}"),
        @OpenApiResponse(status = "400", description = "Invalid phone or no lava-jato"),
        @OpenApiResponse(status = "503", description = "No e-mail channel or mail server down")
      })
  private static void registerPasswordReset(ResetPasswordByEmail reset) {
    post(
        "/api/auth/customers/password-reset",
        JavalinRouteAdapter.adapt(PasswordResetController.request(reset, Who.CUSTOMER)));
    post(
        "/api/auth/customers/password-reset/confirm",
        JavalinRouteAdapter.adapt(PasswordResetController.confirm(reset, Who.CUSTOMER)));
    post(
        "/api/auth/managers/password-reset",
        JavalinRouteAdapter.adapt(PasswordResetController.request(reset, Who.MANAGER)));
    post(
        "/api/auth/managers/password-reset/confirm",
        JavalinRouteAdapter.adapt(PasswordResetController.confirm(reset, Who.MANAGER)));
  }

  @OpenApi(
      path = "/api/customers/{id}/recovery-email",
      methods = {HttpMethod.GET, HttpMethod.PATCH},
      summary = "The customer's own e-mail for password-reset codes (\"Meus dados\")",
      tags = {"Access"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "{email}"),
        @OpenApiResponse(status = "400", description = "Invalid e-mail"),
        @OpenApiResponse(status = "403", description = "Not your account")
      })
  private static void registerRecoveryEmail(CustomerRecoveryEmail recoveryEmail) {
    get(
        "/api/customers/{id}/recovery-email",
        JavalinRouteAdapter.adapt(CustomerRecoveryEmailController.load(recoveryEmail)));
    patch(
        "/api/customers/{id}/recovery-email",
        JavalinRouteAdapter.adapt(CustomerRecoveryEmailController.update(recoveryEmail)));
  }
}
