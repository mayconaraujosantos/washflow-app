package com.washflow.application.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for the password (and PIN) sign-in routes. A failed attempt is a {@code
 * 401} from the endpoint; it counts against the account (route + lava-jato + phone or e-mail) and
 * against the caller's IP.
 *
 * <ul>
 *   <li>{@value #MAX_ACCOUNT_FAILURES} failures for one account lock it for {@link #LOCK}, whatever
 *       the IP - the password is not even checked while locked.
 *   <li>{@value #MAX_IP_FAILURES} failures from one IP (spraying many accounts) lock that IP for
 *       the same time.
 *   <li>A successful sign-in clears the account's failures.
 * </ul>
 *
 * Kept in memory: enough for a single API replica. With more replicas each one counts on its own,
 * which still bounds an attacker to replicas × limit.
 */
public final class LoginThrottle {

  private static final String EMAIL = "email";

  static final int MAX_ACCOUNT_FAILURES = 5;
  static final int MAX_IP_FAILURES = 30;
  static final Duration LOCK = Duration.ofMinutes(15);
  private static final int MAX_ENTRIES = 10_000;
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> ROUTES =
      Set.of(
          "/api/auth/customers",
          "/api/auth/washers",
          "/api/auth/washers/pin",
          "/api/auth/managers",
          "/api/auth/admins");

  private final Clock clock;
  private final Map<String, Failures> failures = new ConcurrentHashMap<>();

  public LoginThrottle(Clock clock) {
    this.clock = clock;
  }

  public void handle(String method, Context ctx, Handler next) throws Exception {
    String route = ctx.path();
    if (!"POST".equals(method) || !ROUTES.contains(route)) {
      next.handle(ctx);
      return;
    }
    Instant now = Instant.now(clock);
    String ipKey = "ip|" + clientIp(ctx);
    String accountKey = accountKey(route, ctx);

    Instant lockedUntil = later(lockedUntil(ipKey, now), lockedUntil(accountKey, now));
    if (lockedUntil != null) {
      long seconds = Math.max(1, Duration.between(now, lockedUntil).toSeconds());
      ctx.status(429)
          .header("Retry-After", Long.toString(seconds))
          .json(
              HttpHelper.errorBody(
                  "Muitas tentativas de acesso. Tente de novo em "
                      + Math.max(1, (seconds + 59) / 60)
                      + " minutos."));
      return;
    }

    next.handle(ctx);
    int status = ctx.statusCode();
    if (status == 401) {
      fail(ipKey, MAX_IP_FAILURES, now);
      if (accountKey != null) {
        fail(accountKey, MAX_ACCOUNT_FAILURES, now);
      }
    } else if (status >= 200 && status < 300 && accountKey != null) {
      failures.remove(accountKey);
    }
  }

  private Instant lockedUntil(String key, Instant now) {
    if (key == null) {
      return null;
    }
    Failures current = failures.get(key);
    return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(now)
        ? current.lockedUntil()
        : null;
  }

  private void fail(String key, int limit, Instant now) {
    failures.compute(
        key,
        (k, current) -> {
          boolean fresh =
              current == null
                  || current.windowStart().plus(LOCK).isBefore(now)
                  || current.lockedUntil() != null;
          int count = fresh ? 1 : current.count() + 1;
          Instant start = fresh ? now : current.windowStart();
          return count >= limit
              ? new Failures(0, now, now.plus(LOCK))
              : new Failures(count, start, null);
        });
    if (failures.size() > MAX_ENTRIES) {
      failures
          .entrySet()
          .removeIf(
              entry -> {
                Failures value = entry.getValue();
                Instant end =
                    value.lockedUntil() != null
                        ? value.lockedUntil()
                        : value.windowStart().plus(LOCK);
                return end.isBefore(now);
              });
    }
  }

  /**
   * The ingress (and Render) put the caller's address as the last X-Forwarded-For hop; earlier hops
   * come from the client and can be forged, so they are ignored.
   */
  static String clientIp(Context ctx) {
    String forwarded = ctx.header("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String[] hops = forwarded.split(",");
      String last = hops[hops.length - 1].trim();
      if (!last.isEmpty()) {
        return last;
      }
    }
    return ctx.ip();
  }

  /** Phone as digits only, e-mail lower-cased; scoped by route and lava-jato. Null if absent. */
  private static String accountKey(String route, Context ctx) {
    JsonNode body;
    try {
      body = JSON.readTree(ctx.body());
    } catch (Exception e) {
      return null;
    }
    if (body == null || !body.isObject()) {
      return null;
    }
    String account = null;
    if (body.hasNonNull(EMAIL) && !body.get(EMAIL).asText().isBlank()) {
      account = body.get(EMAIL).asText().trim().toLowerCase(Locale.ROOT);
    } else if (body.hasNonNull("phone")) {
      account = body.get("phone").asText().replaceAll("\\D", "");
    }
    if (account == null || account.isEmpty()) {
      return null;
    }
    String establishment = ctx.header("X-Establishment");
    return "account|" + route + "|" + (establishment == null ? "-" : establishment) + "|" + account;
  }

  private static Instant later(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isAfter(b) ? a : b;
  }

  private record Failures(int count, Instant windowStart, Instant lockedUntil) {}
}
