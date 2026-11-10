package com.washflow.application.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.washflow.application.security.AuthTokenService;
import com.washflow.data.tenancy.TenantContext;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Who changed what and when: every authenticated {@code POST}/{@code PUT}/{@code PATCH}/{@code
 * DELETE} that succeeds (2xx) is written to the audit trail - a price change, a cash entry edited
 * or deleted, a photo removed, a profile changed, a finalized payment. New endpoints are covered
 * without extra code.
 *
 * <ul>
 *   <li>The request body is kept with passwords, PINs, tokens, codes and images masked, cut to
 *       {@value #MAX_DETAILS} characters.
 *   <li>Runs inside {@code IdempotencyGuard}: a replayed retry is not recorded twice.
 *   <li>Written after the operation, outside its transaction: if the trail cannot be written the
 *       operation still stands and the failure is logged as an error.
 * </ul>
 */
public final class AuditTrail {

  static final int MAX_DETAILS = 2000;
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrail.class);
  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";
  private static final Set<String> CHANGES = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final Pattern SECRET_KEY =
      Pattern.compile(
          ".*(password|senha|pin|token|secret|code|codigo|credential|image|photo|foto|signature"
              + "|assinatura).*");
  private static final ObjectMapper JSON = new ObjectMapper();

  private final AuditStore store;
  private final Clock clock;

  public AuditTrail(AuditStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public void handle(String method, String route, Context ctx, Handler next) throws Exception {
    next.handle(ctx);
    AuthTokenService.Claims claims = ctx.attribute(CLAIMS_ATTRIBUTE);
    int status = ctx.statusCode();
    if (claims == null || !CHANGES.contains(method) || status < 200 || status >= 300) {
      return;
    }
    try {
      store.record(
          new AuditStore.Entry(
              TenantContext.current().orElse(null),
              claims.userId(),
              claims.profile().name(),
              method,
              route,
              ctx.path(),
              status,
              details(ctx.body()),
              Instant.now(clock)));
    } catch (Exception e) {
      LOGGER.error("Audit entry lost for {} {} by {}", method, ctx.path(), claims.userId(), e);
    }
  }

  static String details(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    String text;
    try {
      JsonNode node = JSON.readTree(body);
      mask(node);
      text = JSON.writeValueAsString(node);
    } catch (Exception e) {
      // Not JSON: could be anything, so nothing of it is kept.
      return "";
    }
    return text.length() > MAX_DETAILS ? text.substring(0, MAX_DETAILS - 3) + "..." : text;
  }

  private static void mask(JsonNode node) {
    if (node instanceof ObjectNode object) {
      List<String> names = new ArrayList<>();
      object.fieldNames().forEachRemaining(names::add);
      for (String name : names) {
        if (SECRET_KEY.matcher(name.toLowerCase(Locale.ROOT)).matches()) {
          object.put(name, "***");
        } else {
          mask(object.get(name));
        }
      }
    } else if (node != null && node.isArray()) {
      node.forEach(AuditTrail::mask);
    }
  }
}
