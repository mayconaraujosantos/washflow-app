package com.washflow.application.observability;

import com.washflow.application.security.AuthTokenService;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.errors.common.DomainError;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.HttpResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Wraps every API endpoint: assigns a request id (reusing a sane incoming {@code X-Request-Id}),
 * returns it in the response header, catches anything a handler leaks as a JSON 500, writes one
 * access-log line per request (level by status) and records {@code http.server.requests}.
 */
public final class RequestTracing {

  public static final String HEADER = "X-Request-Id";
  private static final Logger ACCESS = LoggerFactory.getLogger("http");
  private static final Logger ERRORS = LoggerFactory.getLogger(RequestTracing.class);
  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
  private static final SecureRandom RANDOM = new SecureRandom();
  // Probes polled by the platform every few seconds: kept out of the INFO log.
  private static final Set<String> QUIET_ROUTES = Set.of("/api/health", "/api/metrics");
  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";

  private final MeterRegistry registry;

  public RequestTracing(MeterRegistry registry) {
    this.registry = registry;
  }

  public void handle(String method, String route, Context ctx, Handler next) {
    String requestId = requestId(ctx.header(HEADER));
    MDC.put(HttpHelper.REQUEST_ID, requestId);
    ctx.header(HEADER, requestId);
    long started = System.nanoTime();
    try {
      next.handle(ctx);
    } catch (DomainError e) {
      // A business rule broken outside of a controller (e.g. a query without a lava-jato).
      HttpResponse response = HttpHelper.domainError(e);
      ctx.status(response.statusCode()).json(response.body());
    } catch (Exception e) {
      ERRORS.error("Unhandled error method={} route={}", method, route, e);
      ctx.status(500).json(HttpHelper.errorBody("Internal server error"));
    } finally {
      TenantContext.clear();
      long elapsed = System.nanoTime() - started;
      int status = ctx.statusCode();
      log(method, route, ctx, status, TimeUnit.NANOSECONDS.toMillis(elapsed));
      Timer.builder("http.server.requests")
          .description("API requests by route and status")
          .tag("method", method)
          .tag("route", route)
          .tag("status", String.valueOf(status))
          .tag("outcome", outcome(status))
          .register(registry)
          .record(elapsed, TimeUnit.NANOSECONDS);
      MDC.clear();
    }
  }

  private static void log(String method, String route, Context ctx, int status, long millis) {
    AuthTokenService.Claims claims = ctx.attribute(CLAIMS_ATTRIBUTE);
    String user = claims == null ? "-" : claims.userId() + " profile=" + claims.profile();
    String reason = MDC.get(HttpHelper.ERROR_REASON);
    String line =
        "method={} route={} path={} status={} duration_ms={} user={} ip={}"
            + (reason == null ? "" : " reason=\"{}\"");
    Object[] args =
        reason == null
            ? new Object[] {method, route, ctx.path(), status, millis, user, ctx.ip()}
            : new Object[] {method, route, ctx.path(), status, millis, user, ctx.ip(), reason};
    if (status >= 500) {
      ACCESS.error(line, args);
    } else if (status >= 400) {
      ACCESS.warn(line, args);
    } else if (QUIET_ROUTES.contains(route)) {
      ACCESS.debug(line, args);
    } else {
      ACCESS.info(line, args);
    }
  }

  static String requestId(String incoming) {
    if (incoming != null && VALID_ID.matcher(incoming).matches()) {
      return incoming;
    }
    byte[] bytes = new byte[6];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static String outcome(int status) {
    if (status >= 500) {
      return "SERVER_ERROR";
    }
    if (status >= 400) {
      return "CLIENT_ERROR";
    }
    return "SUCCESS";
  }
}
