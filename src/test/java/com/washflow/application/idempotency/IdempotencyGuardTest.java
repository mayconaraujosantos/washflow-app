package com.washflow.application.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.application.security.AuthTokenService;
import com.washflow.domain.entities.user.UserProfile;
import io.javalin.http.Context;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IdempotencyGuardTest {

  private static final UUID USER = UUID.randomUUID();
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-04T14:00:00Z"), ZoneOffset.UTC);

  private final InMemoryStore store = new InMemoryStore();
  private final IdempotencyGuard guard = new IdempotencyGuard(store, CLOCK);
  private final AtomicInteger runs = new AtomicInteger();

  @Test
  void requestsWithoutTheHeaderRunAsBefore() throws Exception {
    Context ctx = request(null, "{}");

    guard.handle("POST", ctx, this::createSale);
    guard.handle("POST", ctx, this::createSale);

    assertEquals(2, runs.get());
    assertEquals(0, store.rows.size());
  }

  @Test
  void aRetryWithTheSameKeyReplaysTheFirstResponseInsteadOfRunningAgain() throws Exception {
    Context first = request("venda-0001", "{\"total\":50}");
    when(first.statusCode()).thenReturn(200);
    when(first.result()).thenReturn("{\"id\":\"sale-1\"}");
    guard.handle("POST", first, this::createSale);

    Context retry = request("venda-0001", "{\"total\":50}");
    guard.handle("POST", retry, this::createSale);

    assertEquals(1, runs.get(), "the sale is created once");
    verify(retry).status(200);
    verify(retry).header(IdempotencyGuard.REPLAYED_HEADER, "true");
    verify(retry).result("{\"id\":\"sale-1\"}");
  }

  @Test
  void theSameKeyWithAnotherBodyIsRefused() throws Exception {
    Context first = request("venda-0001", "{\"total\":50}");
    when(first.statusCode()).thenReturn(200);
    when(first.result()).thenReturn("{}");
    guard.handle("POST", first, this::createSale);

    Context other = request("venda-0001", "{\"total\":80}");
    guard.handle("POST", other, this::createSale);

    assertEquals(1, runs.get());
    verify(other).status(422);
  }

  @Test
  void theSameKeyWhileTheFirstIsStillRunningGetsAConflict() throws Exception {
    store.rows.put(USER + "|venda-0001", new IdempotencyStore.InProgress(hash("{}")));

    Context second = request("venda-0001", "{}");
    guard.handle("POST", second, this::createSale);

    assertEquals(0, runs.get());
    verify(second).status(409);
  }

  @Test
  void failuresAreNotRememberedSoTheRetryRunsAgain() throws Exception {
    Context refused = request("venda-0001", "{}");
    when(refused.statusCode()).thenReturn(400);
    guard.handle("POST", refused, this::createSale);

    Context crashed = request("venda-0001", "{}");
    assertThrows(
        IllegalStateException.class,
        () ->
            guard.handle(
                "POST",
                crashed,
                () -> {
                  throw new IllegalStateException("db down");
                }));

    assertEquals(0, store.rows.size(), "both attempts released the key");
  }

  @Test
  void malformedKeysAreRejected() throws Exception {
    Context ctx = request("x", "{}");

    guard.handle("POST", ctx, this::createSale);

    assertEquals(0, runs.get());
    verify(ctx).status(400);
  }

  @Test
  void onlyAuthenticatedPostsAreCovered() throws Exception {
    Context anonymous = request("cadastro-0001", "{}");
    when(anonymous.attribute("washflow.auth.claims")).thenReturn(null);
    guard.handle("POST", anonymous, this::createSale);
    guard.handle("POST", anonymous, this::createSale);

    Context patch = request("patch-0001", "{}");
    guard.handle("PATCH", patch, this::createSale);

    assertEquals(3, runs.get());
    verify(anonymous, never()).status(409);
  }

  private void createSale() {
    runs.incrementAndGet();
  }

  private static Context request(String key, String body) {
    Context ctx = mock(Context.class, RETURNS_SELF);
    when(ctx.header(IdempotencyGuard.HEADER)).thenReturn(key);
    when(ctx.attribute("washflow.auth.claims"))
        .thenReturn(new AuthTokenService.Claims(USER, UserProfile.MANAGER));
    when(ctx.path()).thenReturn("/api/sales");
    when(ctx.body()).thenReturn(body);
    return ctx;
  }

  private static String hash(String body) {
    return IdempotencyGuard.sha256("POST /api/sales\n" + body);
  }

  /** Same contract as JdbiIdempotencyStore, minus expiry. */
  private static final class InMemoryStore implements IdempotencyStore {
    private final Map<String, Claim> rows = new HashMap<>();

    @Override
    public Claim claim(
        UUID userId,
        String key,
        UUID establishmentId,
        String method,
        String route,
        String requestHash,
        Instant now) {
      Claim existing = rows.putIfAbsent(userId + "|" + key, new InProgress(requestHash));
      return existing == null ? new Acquired() : existing;
    }

    @Override
    public void complete(UUID userId, String key, int status, String body) {
      InProgress running = (InProgress) rows.get(userId + "|" + key);
      rows.put(userId + "|" + key, new Replay(running.requestHash(), status, body));
    }

    @Override
    public void release(UUID userId, String key) {
      rows.computeIfPresent(userId + "|" + key, (k, v) -> v instanceof InProgress ? null : v);
    }
  }
}
