package com.washflow.main.adapters;

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

  // Lets the shared 404 error handler in Main tell "a controller matched and
  // deliberately returned 404" (leave its JSON body alone) apart from "no
  // route matched at all" (Javalin already wrote its own default body by
  // then, so checking ctx.result() can't tell the two apart).
  public static final String CONTROLLER_HANDLED_ATTRIBUTE = "controllerHandled";

  private JavalinRouteAdapter() {}

  @SuppressWarnings("unchecked")
  public static Handler adapt(Controller controller) {
    return ctx -> {
      Map<String, Object> body;
      try {
        body = ctx.bodyAsClass(Map.class);
      } catch (RuntimeException e) {
        body = Map.of();
      }

      HttpRequest request = new HttpRequest(body, ctx.pathParamMap());
      HttpResponse response = controller.handle(request);

      ctx.attribute(CONTROLLER_HANDLED_ATTRIBUTE, Boolean.TRUE);
      ctx.status(response.statusCode()).json(response.body());
    };
  }
}
