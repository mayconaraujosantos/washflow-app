package com.washflow.application.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class RequestTracingTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final RequestTracing tracing = new RequestTracing(registry);

  private static Context context(String incomingId, int status) {
    Context ctx = mock(Context.class);
    when(ctx.header(RequestTracing.HEADER)).thenReturn(incomingId);
    when(ctx.statusCode()).thenReturn(status);
    when(ctx.path()).thenReturn("/api/service-orders/abc/start");
    when(ctx.ip()).thenReturn("10.0.0.1");
    when(ctx.status(anyInt())).thenReturn(ctx);
    return ctx;
  }

  @Test
  void reusesASaneIncomingRequestIdAndReturnsIt() {
    Context ctx = context("abc12345-front", 200);
    String[] seen = new String[1];

    tracing.handle(
        "PATCH", "/api/service-orders/{id}/start", ctx, ignored -> seen[0] = MDC.get("request_id"));

    assertEquals("abc12345-front", seen[0]);
    verify(ctx).header(RequestTracing.HEADER, "abc12345-front");
    assertNull(MDC.get("request_id"), "MDC is cleared after the request");
  }

  @Test
  void generatesAnIdWhenTheIncomingOneIsMissingOrUnsafe() {
    assertEquals(12, RequestTracing.requestId(null).length());
    assertEquals(12, RequestTracing.requestId("bad id\nwith newline").length());
  }

  @Test
  void turnsLeakedExceptionsIntoAJson500WithTheRequestId() {
    Context ctx = context("req-00000001", 500);

    tracing.handle(
        "GET",
        "/api/x",
        ctx,
        ignored -> {
          throw new IllegalStateException("boom");
        });

    verify(ctx).status(500);
    ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
    verify(ctx).json(body.capture());
    Map<?, ?> json = (Map<?, ?>) body.getValue();
    assertEquals("Internal server error", json.get("error"));
    assertEquals("req-00000001", json.get("requestId"));
  }

  @Test
  void recordsRequestMetricsByRouteTemplateAndStatus() {
    tracing.handle("PATCH", "/api/service-orders/{id}/start", context(null, 409), ignored -> {});

    var timer =
        registry
            .find("http.server.requests")
            .tag("route", "/api/service-orders/{id}/start")
            .tag("status", "409")
            .tag("outcome", "CLIENT_ERROR")
            .timer();
    assertNotNull(timer);
    assertEquals(1, timer.count());
    assertTrue(
        registry
            .find("http.server.requests")
            .tag("route", "/api/service-orders/abc/start")
            .timers()
            .isEmpty(),
        "metrics use the route template, never the concrete path");
  }
}
