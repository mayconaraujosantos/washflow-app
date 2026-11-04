package com.washflow.application.idempotency;

import com.washflow.application.security.AuthTokenService;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Makes authenticated {@code POST}s safe to resend: when the request carries an {@code
 * Idempotency-Key} header, the first success is stored and any later request with the same key (a
 * retry after a timeout, a double click) gets that same response back - flagged with {@code
 * Idempotent-Replayed: true} - instead of creating a second sale, cash entry, vehicle entry or
 * booking.
 *
 * <ul>
 *   <li>Keys belong to the authenticated user and expire after 24 hours.
 *   <li>Only 2xx responses are stored; a failed request releases its key so it can be retried.
 *   <li>The same key with a different body or route is refused (422): it's a client bug.
 *   <li>The same key while the first request is still running gets 409 (try again shortly).
 * </ul>
 *
 * Requests without the header behave exactly as before.
 */
public final class IdempotencyGuard {

  public static final String HEADER = "Idempotency-Key";
  public static final String REPLAYED_HEADER = "Idempotent-Replayed";
  private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_.:-]{8,100}");
  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";

  /** The endpoint's own work. */
  @FunctionalInterface
  public interface Work {
    void run() throws Exception;
  }

  private final IdempotencyStore store;
  private final Clock clock;

  public IdempotencyGuard(IdempotencyStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public void handle(String method, Context ctx, Work work) throws Exception {
    String key = ctx.header(HEADER);
    AuthTokenService.Claims claims = ctx.attribute(CLAIMS_ATTRIBUTE);
    if (key == null || !"POST".equals(method) || claims == null) {
      work.run();
      return;
    }
    if (!VALID_KEY.matcher(key).matches()) {
      ctx.status(400)
          .json(
              HttpHelper.errorBody(
                  "Idempotency-Key inválida: use de 8 a 100 letras, números, '-', '_', '.' ou ':'"));
      return;
    }

    String route = ctx.path();
    String requestHash = sha256(method + " " + route + "\n" + ctx.body());
    IdempotencyStore.Claim claim =
        store.claim(
            claims.userId(),
            key,
            TenantContext.current().orElse(null),
            method,
            route,
            requestHash,
            Instant.now(clock));

    switch (claim) {
      case IdempotencyStore.Replay replay -> {
        if (!replay.requestHash().equals(requestHash)) {
          reusedKey(ctx);
          return;
        }
        ctx.status(replay.status())
            .header(REPLAYED_HEADER, "true")
            .contentType("application/json")
            .result(replay.body() == null ? "" : replay.body());
      }
      case IdempotencyStore.InProgress inProgress -> {
        if (!inProgress.requestHash().equals(requestHash)) {
          reusedKey(ctx);
          return;
        }
        ctx.status(409)
            .json(
                HttpHelper.errorBody(
                    "Esta operação ainda está sendo processada. Aguarde um instante e confira"
                        + " antes de tentar de novo."));
      }
      case IdempotencyStore.Acquired acquired -> runAndRemember(ctx, claims, key, work);
    }
  }

  private void runAndRemember(Context ctx, AuthTokenService.Claims claims, String key, Work work)
      throws Exception {
    boolean stored = false;
    try {
      work.run();
      int status = ctx.statusCode();
      if (status >= 200 && status < 300) {
        store.complete(claims.userId(), key, status, ctx.result());
        stored = true;
      }
    } finally {
      if (!stored) {
        store.release(claims.userId(), key);
      }
    }
  }

  private static void reusedKey(Context ctx) {
    ctx.status(422)
        .json(
            HttpHelper.errorBody(
                "Esta Idempotency-Key já foi usada para outra requisição. Gere uma chave nova"
                    + " para cada operação."));
  }

  static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
