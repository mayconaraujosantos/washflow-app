package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;

import com.washflow.application.security.AuthTokenService;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.presentation.controllers.operations.PauseServiceOrderRequest;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public final class OperationsRoutes {

  private OperationsRoutes() {}

  public static void register(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    patch("/api/service-orders/{id}/pause", ctx -> pause(ctx, repository));
    patch("/api/service-orders/{id}/resume", ctx -> resume(ctx, repository));
    get("/api/operations/queue", ctx -> ctx.json(repository.loadWaitingQueue()));
    get("/api/operations/yard", ctx -> ctx.json(repository.loadYardOverview()));
    get("/api/operations/report", ctx -> report(ctx, repository));
    get(
        "/api/operations/my-orders",
        ctx -> {
          AuthTokenService.Claims claims = ctx.attribute("washflow.auth.claims");
          ctx.json(repository.loadWashingByWasher(claims.userId()));
        });
  }

  private static void pause(Context ctx, ServiceOrderJdbiRepository repository) {
    UUID orderId;
    try {
      orderId = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Invalid service order id"));
      return;
    }
    AuthTokenService.Claims claims = ctx.attribute("washflow.auth.claims");
    PauseServiceOrderRequest request = ctx.bodyAsClass(PauseServiceOrderRequest.class);
    String reason = request.reason() == null ? "" : request.reason().trim();
    String description = request.description() == null ? null : request.description().trim();
    if (!Set.of("LUNCH", "RESTROOM", "OTHER").contains(reason)
        || "OTHER".equals(reason) && (description == null || description.isBlank())
        || description != null && description.length() > 200) {
      ctx.status(400)
          .json(
              Map.of(
                  "error",
                  "Choose a valid pause reason; OTHER requires a description of up to 200 characters"));
      return;
    }
    if (description != null && description.isBlank()) {
      description = null;
    }
    if (!repository.pauseWashing(orderId, claims.userId(), reason, description)) {
      ctx.status(409).json(Map.of("error", "Service order is not an active wash assigned to you"));
      return;
    }
    ctx.json(Map.of("status", "PAUSED"));
  }

  private static void resume(Context ctx, ServiceOrderJdbiRepository repository) {
    UUID orderId;
    try {
      orderId = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Invalid service order id"));
      return;
    }
    AuthTokenService.Claims claims = ctx.attribute("washflow.auth.claims");
    if (!repository.resumeWashing(orderId, claims.userId())) {
      ctx.status(409)
          .json(Map.of("error", "Service order is not paused or is not assigned to you"));
      return;
    }
    ctx.json(Map.of("status", "WASHING"));
  }

  private static void report(Context ctx, ServiceOrderJdbiRepository repository) {
    String fromParameter = ctx.queryParam("from");
    String toParameter = ctx.queryParam("to");
    if (fromParameter == null || toParameter == null) {
      ctx.status(400).json(Map.of("error", "Provide from and to as YYYY-MM-DD dates"));
      return;
    }
    LocalDate from;
    LocalDate to;
    try {
      from = LocalDate.parse(fromParameter);
      to = LocalDate.parse(toParameter);
    } catch (DateTimeParseException e) {
      ctx.status(400).json(Map.of("error", "Provide from and to as YYYY-MM-DD dates"));
      return;
    }
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365) {
      ctx.status(400)
          .json(Map.of("error", "Report range must be ordered and no longer than 366 days"));
      return;
    }
    ctx.json(repository.loadOperationsReport(from, to));
  }
}
