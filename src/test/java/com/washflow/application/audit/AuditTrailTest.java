package com.washflow.application.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.application.security.AuthTokenService;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.user.UserProfile;
import io.javalin.http.Context;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuditTrailTest {

  private static final UUID MANAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID LAVA = UUID.fromString("eeeeeeee-eeee-eeee-eeee-000000000001");
  private static final Instant NOW = Instant.parse("2026-11-10T14:00:00Z");

  private final InMemoryStore store = new InMemoryStore();
  private final AuditTrail trail = new AuditTrail(store, Clock.fixed(NOW, ZoneOffset.UTC));
  private final AtomicInteger runs = new AtomicInteger();

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void recordsWhoChangedWhatAndWhen() throws Exception {
    TenantContext.set(LAVA);
    Context ctx =
        request("/api/cash-transactions/abc", "{\"amount\":49.90,\"description\":\"Troco\"}", 200);

    trail.handle("PUT", "/api/cash-transactions/{id}", ctx, this::work);

    assertEquals(1, runs.get());
    AuditStore.Entry entry = store.entries.get(0);
    assertEquals(LAVA, entry.establishmentId());
    assertEquals(MANAGER, entry.userId());
    assertEquals("MANAGER", entry.profile());
    assertEquals("PUT", entry.method());
    assertEquals("/api/cash-transactions/{id}", entry.route());
    assertEquals("/api/cash-transactions/abc", entry.path());
    assertEquals(200, entry.status());
    assertEquals(NOW, entry.at());
    assertTrue(entry.details().contains("\"amount\":49.9"), entry.details());
  }

  @Test
  void secretsAndPhotosNeverReachTheTrail() throws Exception {
    Context ctx =
        request(
            "/api/users",
            "{\"name\":\"Rui\",\"password\":\"segredo123\",\"pin\":\"123456\","
                + "\"nested\":{\"badgeToken\":\"t\",\"imageData\":\"data:image/png;base64,AAA\"}}",
            201);

    trail.handle("POST", "/api/users", ctx, this::work);

    String details = store.entries.get(0).details();
    assertTrue(details.contains("Rui"), details);
    for (String secret : new String[] {"segredo123", "123456", "\"t\"", "base64"}) {
      assertFalse(details.contains(secret), secret + " leaked: " + details);
    }
    assertTrue(details.contains("\"password\":\"***\""), details);
  }

  @Test
  void readsFailuresAndAnonymousCallsAreNotRecorded() throws Exception {
    trail.handle("GET", "/api/users", request("/api/users", "", 200), this::work);
    trail.handle("POST", "/api/sales", request("/api/sales", "{}", 400), this::work);

    Context anonymous = request("/api/auth/customers", "{}", 200);
    when(anonymous.attribute("washflow.auth.claims")).thenReturn(null);
    trail.handle("POST", "/api/auth/customers", anonymous, this::work);

    assertEquals(3, runs.get());
    assertEquals(0, store.entries.size());
  }

  @Test
  void hugeOrMalformedBodiesAreKeptShortAndSafe() throws Exception {
    trail.handle("POST", "/api/sales", request("/api/sales", "x".repeat(10_000), 200), this::work);
    trail.handle(
        "PATCH",
        "/api/users/{id}/active",
        request("/api/users/1/active", "{\"note\":\"" + "y".repeat(10_000) + "\"}", 200),
        this::work);

    assertTrue(store.entries.get(0).details().length() <= AuditTrail.MAX_DETAILS);
    assertTrue(store.entries.get(1).details().length() <= AuditTrail.MAX_DETAILS);
  }

  @Test
  void aBrokenAuditStoreDoesNotUndoTheOperation() throws Exception {
    AuditTrail failing =
        new AuditTrail(
            new InMemoryStore() {
              @Override
              public void record(Entry entry) {
                throw new IllegalStateException("db down");
              }
            },
            Clock.fixed(NOW, ZoneOffset.UTC));

    failing.handle("POST", "/api/sales", request("/api/sales", "{}", 201), this::work);

    assertEquals(1, runs.get());
  }

  @Test
  void handlerErrorsPropagateWithoutARecord() {
    assertThrows(
        IllegalStateException.class,
        () ->
            trail.handle(
                "POST",
                "/api/sales",
                request("/api/sales", "{}", 200),
                ignored -> {
                  throw new IllegalStateException("boom");
                }));
    assertEquals(0, store.entries.size());
  }

  private void work(Context ignored) {
    runs.incrementAndGet();
  }

  private static Context request(String path, String body, int status) {
    Context ctx = mock(Context.class, RETURNS_SELF);
    when(ctx.attribute("washflow.auth.claims"))
        .thenReturn(new AuthTokenService.Claims(MANAGER, UserProfile.MANAGER));
    when(ctx.path()).thenReturn(path);
    when(ctx.body()).thenReturn(body);
    when(ctx.statusCode()).thenReturn(status);
    return ctx;
  }

  private static class InMemoryStore implements AuditStore {
    final List<Entry> entries = new ArrayList<>();

    @Override
    public void record(Entry entry) {
      entries.add(entry);
    }

    @Override
    public List<Row> list(LocalDate from, LocalDate to) {
      return List.of();
    }
  }
}
