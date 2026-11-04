package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.observability.ClientErrorLog;
import com.washflow.infra.observability.Metrics;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Map;

/** Prometheus scrape endpoint and the browser error sink. */
public final class ObservabilityRoutes {

  private static final ClientErrorLog CLIENT_ERRORS = new ClientErrorLog(Clock.systemUTC());

  private ObservabilityRoutes() {}

  public static void register(AppConfig appConfig) {
    registerMetrics(appConfig.metricsToken());
    registerClientErrors();
  }

  @OpenApi(
      path = "/api/metrics",
      methods = HttpMethod.GET,
      summary = "Prometheus metrics (Authorization: Bearer METRICS_TOKEN; off when unset)",
      tags = {"Observability"},
      responses = {
        @OpenApiResponse(status = "200", description = "Prometheus text format"),
        @OpenApiResponse(status = "401", description = "Missing or wrong token"),
        @OpenApiResponse(status = "404", description = "METRICS_TOKEN not configured")
      })
  private static void registerMetrics(String token) {
    get("/api/metrics", ctx -> serveMetrics(ctx, token));
  }

  static void serveMetrics(Context ctx, String token) {
    if (token == null || token.isBlank()) {
      ctx.status(404).json(HttpHelper.errorBody("Not found"));
      return;
    }
    String header = ctx.header("Authorization");
    String given = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    if (!MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
      ctx.status(401).json(HttpHelper.errorBody("Invalid metrics token"));
      return;
    }
    ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(Metrics.registry().scrape());
  }

  @OpenApi(
      path = "/api/client-errors",
      methods = HttpMethod.POST,
      summary = "Report a browser error from the PWA (public, rate limited)",
      tags = {"Observability"},
      responses = {
        @OpenApiResponse(status = "204", description = "Logged"),
        @OpenApiResponse(status = "429", description = "Too many reports")
      })
  private static void registerClientErrors() {
    post(
        "/api/client-errors",
        ctx -> {
          Map<String, Object> body;
          try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = ctx.bodyAsClass(Map.class);
            body = parsed;
          } catch (Exception e) {
            body = Map.of("message", "unreadable report");
          }
          if (CLIENT_ERRORS.record(ctx.ip(), body)) {
            ctx.status(204);
          } else {
            ctx.status(429).json(HttpHelper.errorBody("Too many error reports"));
          }
        });
  }
}
