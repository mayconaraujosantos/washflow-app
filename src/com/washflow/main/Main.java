package com.washflow.main;

import static io.javalin.apibuilder.ApiBuilder.get;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    Javalin app =
        Javalin.create(
            config -> {
              config.router.ignoreTrailingSlashes = true;

              // The built PWA (web/dist) is copied onto the classpath under
              // /static by the processResources Gradle task - not into
              // src/main/resources on disk - so this must check the classpath,
              // not the filesystem, and Location.CLASSPATH already no-ops
              // gracefully when nothing is there.
              config.staticFiles.add(
                  staticFiles -> {
                    staticFiles.directory = "/static";
                    staticFiles.location = Location.CLASSPATH;
                    // Jetty's default mime table doesn't know .webmanifest;
                    // without this Chrome refuses to treat it as installable.
                    staticFiles.mimeTypes.add("application/manifest+json", "webmanifest");
                  });

              config.routes.apiBuilder(
                  () -> {
                    get(
                        "/api/health",
                        ctx -> ctx.json(Map.of("status", "ok", "service", "washflow-api")));
                    get(
                        "/api/hello",
                        ctx -> ctx.json(Map.of("message", "Hello from Javalin + React PWA")));
                  });

              // Real files (JS bundle, manifest, service worker) are already
              // served by the static handler above. Anything else falls here -
              // either a client-side route the SPA should handle, or a
              // genuinely missing API endpoint.
              config.routes.error(
                  404,
                  ctx -> {
                    if (ctx.path().startsWith("/api/")) {
                      ctx.contentType("text/plain").result("Not found");
                      return;
                    }

                    var resource = Main.class.getResource("/static/index.html");
                    if (resource == null) {
                      ctx.contentType("text/html")
                          .result(
                              "<html><body><h1>Washflow API</h1><p>Backend is running. Frontend build not generated yet.</p></body></html>");
                      return;
                    }

                    try (var input = resource.openStream()) {
                      ctx.contentType("text/html")
                          .result(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    }
                  });
            });

    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
    app.start(port);
  }
}
