package com.washflow.application.adapters;

import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;

/**
 * The shared 404 handler registered via {@code config.routes.error(404, ...)} in Application. Real
 * files (JS bundle, manifest, service worker) are already served by the static handler in
 * Application - anything else falls here: either a client-side route the SPA should handle, a
 * genuinely missing API endpoint, or a matched route/controller that deliberately returned 404
 * (e.g. {@code HttpHelper.notFound()}) - that last case already wrote its own JSON body, which this
 * handler must leave alone instead of overwriting.
 */
public final class SpaFallbackHandler {

  private SpaFallbackHandler() {}

  public static void handle(Context ctx) throws Exception {
    if (ctx.attribute(JavalinRouteAdapter.CONTROLLER_HANDLED_ATTRIBUTE) != null) {
      return;
    }

    if (ctx.path().startsWith("/api/")) {
      ctx.contentType("text/plain").result("Not found");
      return;
    }

    var resource = SpaFallbackHandler.class.getResource("/static/index.html");
    if (resource == null) {
      ctx.contentType("text/html")
          .result(
              "<html><body><h1>Washflow API</h1><p>Backend is running. Frontend build not generated yet.</p></body></html>");
      return;
    }

    try (var input = resource.openStream()) {
      ctx.contentType("text/html").result(new String(input.readAllBytes(), StandardCharsets.UTF_8));
    }
  }
}
