package com.washflow.application;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.washflow.application.adapters.SpaFallbackHandler;
import com.washflow.application.adapters.StaticCachePolicy;
import com.washflow.application.idempotency.IdempotencyGuard;
import com.washflow.application.observability.RequestTracing;
import com.washflow.application.routes.AccessRoutes;
import com.washflow.application.routes.EstablishmentRoutes;
import com.washflow.application.routes.FeatureRoutes;
import com.washflow.application.routes.ObservabilityRoutes;
import com.washflow.application.routes.OperationsRoutes;
import com.washflow.application.routes.PlatformRoutes;
import com.washflow.application.routes.SaleRoutes;
import com.washflow.application.routes.ScheduleRoutes;
import com.washflow.application.routes.ServiceOrderRoutes;
import com.washflow.application.routes.ServicePriceRoutes;
import com.washflow.application.routes.SystemRoutes;
import com.washflow.application.routes.UserRoutes;
import com.washflow.application.routes.VehicleRoutes;
import com.washflow.application.security.AccountSecurityGateway;
import com.washflow.application.security.AuthTokenService;
import com.washflow.application.security.AuthorizationMiddleware;
import com.washflow.application.security.LoginThrottle;
import com.washflow.infra.db.jdbi.JdbiAccountSecurityGateway;
import com.washflow.infra.db.jdbi.JdbiFactory;
import com.washflow.infra.db.jdbi.JdbiIdempotencyStore;
import com.washflow.infra.observability.Metrics;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import java.time.Clock;
import java.util.Map;
import org.jdbi.v3.core.Jdbi;

public class Application {
  public static void main(String[] args) {
    AppConfig appConfig = AppConfig.load();
    Jdbi jdbi = JdbiFactory.create(appConfig);
    AccountSecurityGateway securityGateway = new JdbiAccountSecurityGateway(jdbi);
    AuthorizationMiddleware authorizationMiddleware =
        new AuthorizationMiddleware(
            new AuthTokenService(appConfig.tokenSecret()), securityGateway, jdbi);

    RequestTracing tracing = new RequestTracing(Metrics.registry());
    IdempotencyGuard idempotency =
        new IdempotencyGuard(new JdbiIdempotencyStore(jdbi), Clock.systemUTC());
    LoginThrottle loginThrottle = new LoginThrottle(Clock.systemUTC());

    Javalin app =
        Javalin.create(
            config -> {
              config.router.ignoreTrailingSlashes = true;
              // Request id, access log, metrics and a JSON 500 for anything a
              // handler leaks - around authorization, so denials are logged too.
              config.router.handlerWrapper(
                  endpoint ->
                      ctx ->
                          tracing.handle(
                              endpoint.method.name(),
                              endpoint.path,
                              ctx,
                              () ->
                                  // Sign-in routes: lock an account or IP after repeated 401s.
                                  loginThrottle.handle(
                                      endpoint.method.name(),
                                      ctx,
                                      () -> {
                                        if (authorizationMiddleware.authorize(ctx)) {
                                          // Idempotency-Key: a resent POST replays its first
                                          // success.
                                          idempotency.handle(
                                              endpoint.method.name(),
                                              ctx,
                                              () -> endpoint.handler.handle(ctx));
                                        }
                                      })));
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
              // Three passes over the same folder, each with its own cache
              // headers (see StaticCachePolicy): app shell never cached,
              // hashed bundles cached for a year, everything else revalidated.
              addStaticFiles(
                  config, StaticCachePolicy::isAppShell, StaticCachePolicy.APP_SHELL_HEADERS);
              addStaticFiles(
                  config, StaticCachePolicy::isHashedAsset, StaticCachePolicy.HASHED_ASSET_HEADERS);
              addStaticFiles(config, StaticCachePolicy::isOtherStatic, Map.of());

              config.routes.apiBuilder(
                  () -> {
                    SystemRoutes.register(jdbi);
                    OperationsRoutes.register(jdbi, appConfig);
                    ServiceOrderRoutes.register(jdbi, appConfig);
                    ServicePriceRoutes.register(jdbi);
                    UserRoutes.register(jdbi, appConfig);
                    VehicleRoutes.register(jdbi);
                    FeatureRoutes.register(jdbi);
                    SaleRoutes.register(jdbi);
                    EstablishmentRoutes.register(jdbi, appConfig);
                    AccessRoutes.register(jdbi, appConfig);
                    ScheduleRoutes.register(jdbi, appConfig);
                    ObservabilityRoutes.register(appConfig);
                    PlatformRoutes.register(jdbi, appConfig);
                  });

              config.routes.error(404, SpaFallbackHandler::handle);
            });

    app.start(appConfig.port());
  }

  private static void addStaticFiles(
      io.javalin.config.JavalinConfig config,
      java.util.function.Predicate<String> serves,
      Map<String, String> headers) {
    config.staticFiles.add(
        staticFiles -> {
          staticFiles.directory = "/static";
          staticFiles.location = Location.CLASSPATH;
          staticFiles.headers = headers;
          staticFiles.skipFileFunction = request -> !serves.test(request.getRequestURI());
          // Jetty's default mime table doesn't know .webmanifest;
          // without this Chrome refuses to treat it as installable.
          staticFiles.mimeTypes.add("application/manifest+json", "webmanifest");
        });
  }
}
