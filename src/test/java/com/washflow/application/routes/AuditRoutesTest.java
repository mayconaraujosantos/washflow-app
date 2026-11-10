package com.washflow.application.routes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.application.audit.AuditStore;
import io.javalin.http.Context;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditRoutesTest {

  private final AuditStore store = mock(AuditStore.class);

  @Test
  void listsTheTrailForTheRequestedDates() {
    List<AuditStore.Row> rows =
        List.of(
            new AuditStore.Row(
                Instant.parse("2026-11-10T12:00:00Z"),
                UUID.randomUUID(),
                "Ana",
                "MANAGER",
                "DELETE",
                "/api/cash-transactions/{id}",
                "/api/cash-transactions/abc",
                204,
                ""));
    when(store.list(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10"))).thenReturn(rows);
    Context ctx = request("2026-11-01", "2026-11-10");

    AuditRoutes.serveList(ctx, store);

    verify(ctx).json(rows);
  }

  @Test
  void refusesMissingInvertedOrTooLongRanges() {
    String[][] invalid = {
      {null, "2026-11-10"},
      {"2026-11-10", "ontem"},
      {"2026-11-10", "2026-11-01"},
      {"2026-01-01", "2026-12-31"}
    };
    for (String[] range : invalid) {
      Context ctx = request(range[0], range[1]);

      AuditRoutes.serveList(ctx, store);

      verify(ctx).status(400);
      verify(ctx, never()).json(any(List.class));
    }
  }

  private static Context request(String from, String to) {
    Context ctx = mock(Context.class, RETURNS_SELF);
    when(ctx.queryParam("from")).thenReturn(from);
    when(ctx.queryParam("to")).thenReturn(to);
    return ctx;
  }
}
