package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;

import com.washflow.application.audit.AuditStore;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** The audit trail of the manager's lava-jato (manager only, see RoutePermissionMatcher). */
public final class AuditRoutes {

  static final int MAX_DAYS = 92;

  private AuditRoutes() {}

  public static void register(AuditStore store) {
    registerList(store);
  }

  @OpenApi(
      path = "/api/audit",
      methods = HttpMethod.GET,
      summary = "Who changed what and when, between two Manaus dates (newest first)",
      tags = {"Audit"},
      queryParams = {
        @OpenApiParam(name = "from", type = String.class, required = true),
        @OpenApiParam(name = "to", type = String.class, required = true)
      },
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = AuditStore.Row[].class)),
        @OpenApiResponse(status = "400", description = "Invalid range (at most 92 days)")
      })
  private static void registerList(AuditStore store) {
    get("/api/audit", ctx -> serveList(ctx, store));
  }

  static void serveList(Context ctx, AuditStore store) {
    LocalDate from;
    LocalDate to;
    try {
      from = LocalDate.parse(String.valueOf(ctx.queryParam("from")));
      to = LocalDate.parse(String.valueOf(ctx.queryParam("to")));
    } catch (DateTimeParseException e) {
      ctx.status(400).json(HttpHelper.errorBody("Informe from e to no formato AAAA-MM-DD"));
      return;
    }
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
      ctx.status(400)
          .json(
              HttpHelper.errorBody(
                  "Período inválido: a data final deve ser igual ou depois da inicial, em até "
                      + MAX_DAYS
                      + " dias"));
      return;
    }
    ctx.json(store.list(from, to));
  }
}
