package com.washflow.application;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.washflow.application.adapters.SpaFallbackHandler;
import com.washflow.application.routes.ServiceOrderRoutes;
import com.washflow.application.routes.ServicePriceRoutes;
import com.washflow.application.routes.SystemRoutes;
import com.washflow.application.routes.UserRoutes;
import com.washflow.application.routes.VehicleRoutes;
import com.washflow.infra.db.jdbi.JdbiFactory;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
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
              // the @OpenApi annotations on the route registration methods
              // below, compiled in (no reflection).
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
                    SystemRoutes.register(jdbi);
                    ServiceOrderRoutes.register(jdbi);
                    ServicePriceRoutes.register(jdbi);
                    UserRoutes.register(jdbi);
                    VehicleRoutes.register(jdbi);
                  });

              config.routes.error(404, SpaFallbackHandler::handle);
            });

    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
    app.start(port);
  }
}
