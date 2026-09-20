package com.washflow.application;

import static io.javalin.apibuilder.ApiBuilder.get;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.routes.ServiceOrderRoutes;
import com.washflow.infra.db.jdbi.JdbiFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jdbi.v3.core.Jdbi;

public class Application {
  public static void main(String[] args) {
    Jdbi jdbi = JdbiFactory.create();

    Javalin app =
        Javalin.create(
            config -> {
              config.router.ignoreTrailingSlashes = true;
              // Without JavaTimeModule, java.time.Instant fields (e.g.
              // ServiceOrder's) blow up serialization with an
              // InvalidDefinitionException; without disabling
              // WRITE_DATES_AS_TIMESTAMPS they'd serialize as raw epoch
              // floats instead of ISO-8601 strings.
              config.jsonMapper(
                  new JavalinJackson()
                      .updateMapper(
                          mapper ->
                              mapper
                                  .registerModule(new JavaTimeModule())
                                  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

              // OpenAPI spec at /openapi, Swagger UI at /swagger - built from
              // the @OpenApi annotations on the handler methods below and on
              // ServiceOrderRoutes.register, compiled in (no reflection).
              config.registerPlugin(
                  new OpenApiPlugin(
                      pluginConfig ->
                          pluginConfig.withDefinitionConfiguration(
                              (version, schema) ->
                                  schema.info(
                                      info -> info.title("Washflow API").version("1.0.0")))));
              config.registerPlugin(new SwaggerPlugin());

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
                    get("/api/health", ctx -> health(ctx, jdbi));
                    get("/api/hello", Application::hello);

                    ServiceOrderRoutes.register(jdbi);
                  });

              // Real files (JS bundle, manifest, service worker) are already
              // served by the static handler above. Anything else falls here -
              // either a client-side route the SPA should handle, a genuinely
              // missing API endpoint, or a matched route/controller that
              // deliberately returned 404 (e.g. HttpHelper.notFound()) - that
              // last case already wrote its own JSON body, which this handler
              // must leave alone instead of overwriting.
              config.routes.error(
                  404,
                  ctx -> {
                    if (ctx.attribute(JavalinRouteAdapter.CONTROLLER_HANDLED_ATTRIBUTE) != null) {
                      return;
                    }

                    if (ctx.path().startsWith("/api/")) {
                      ctx.contentType("text/plain").result("Not found");
                      return;
                    }

                    var resource = Application.class.getResource("/static/index.html");
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

  @OpenApi(
      path = "/api/health",
      methods = HttpMethod.GET,
      summary = "Liveness and database connectivity check",
      responses = @OpenApiResponse(status = "200"))
  private static void health(Context ctx, Jdbi jdbi) {
    var body = new LinkedHashMap<String, String>();
    body.put("status", "ok");
    body.put("service", "washflow-api");
    body.put("database", checkDatabase(jdbi));
    ctx.json(body);
  }

  @OpenApi(
      path = "/api/hello",
      methods = HttpMethod.GET,
      summary = "Sample greeting endpoint",
      responses = @OpenApiResponse(status = "200"))
  private static void hello(Context ctx) {
    ctx.json(Map.of("message", "Hello from Javalin + React PWA"));
  }

  private static String checkDatabase(Jdbi jdbi) {
    try {
      jdbi.withHandle(handle -> handle.execute("SELECT 1"));
      return "up";
    } catch (RuntimeException e) {
      return "down";
    }
  }
}
