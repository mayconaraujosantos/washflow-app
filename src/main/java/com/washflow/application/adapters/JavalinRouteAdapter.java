package com.washflow.application.adapters;

import com.washflow.application.security.AuthTokenService;
import com.washflow.application.security.AuthenticatedSession;
import com.washflow.domain.entities.User;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import io.javalin.http.Handler;
import java.util.Map;

/**
 * The only place that converts between Javalin's {@code Context} and the framework-agnostic {@code
 * HttpRequest}/{@code HttpResponse}. If Javalin were ever replaced, this is the only class that
 * would need to change - controllers, use cases and repositories wouldn't.
 */
public final class JavalinRouteAdapter {

  // Lets the shared 404 error handler in Application tell "a controller matched and
  // deliberately returned 404" (leave its JSON body alone) apart from "no
  // route matched at all" (Javalin already wrote its own default body by
  // then, so checking ctx.result() can't tell the two apart).
  public static final String CONTROLLER_HANDLED_ATTRIBUTE = "controllerHandled";

  private JavalinRouteAdapter() {}

  public static Handler adapt(Controller controller) {
    return adapt(controller, response -> response.body());
  }

  public static Handler adaptAuthenticated(Controller controller, AuthTokenService tokenService) {
    return adapt(
        controller,
        response -> {
          if (response.statusCode() == 200 && response.body() instanceof User user) {
            return new AuthenticatedSession(user, tokenService.issue(user));
          }
          return response.body();
        });
  }

  @SuppressWarnings("unchecked")
  private static Handler adapt(
      Controller controller, java.util.function.Function<HttpResponse, Object> responseBody) {
    return ctx -> {
      Map<String, Object> body;
      try {
        body = ctx.bodyAsClass(Map.class);
      } catch (Exception e) {
        // ctx.bodyAsClass throws Jackson's MismatchedInputException on an
        // empty body (e.g. every GET request) - a checked IOException under
        // Jackson's JacksonException, not a RuntimeException, even though
        // Kotlin lets it fly here undeclared. Catching only RuntimeException
        // let it through as an uncaught 500.
        body = Map.of();
      }

      // Inject createdBy from authenticated user claims into the request body
      // so controllers can access it without depending on Javalin Context.
      AuthTokenService.Claims claims = ctx.attribute("washflow.auth.claims");
      if (claims != null) {
        // Use a mutable copy so we can add the createdBy field
        body = new java.util.HashMap<>(body);
        body.put("createdBy", claims.userId());
      }

      // Flatten queryParamMap (Map<String,List<String>>) to Map<String,String> using first value
      Map<String, String> queryParams =
          ctx.queryParamMap().entrySet().stream()
              .collect(
                  java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
      HttpRequest request = new HttpRequest(body, ctx.pathParamMap(), queryParams);
      HttpResponse response = controller.handle(request);

      ctx.attribute(CONTROLLER_HANDLED_ATTRIBUTE, Boolean.TRUE);
      ctx.status(response.statusCode()).json(responseBody.apply(response));
    };
  }
}
