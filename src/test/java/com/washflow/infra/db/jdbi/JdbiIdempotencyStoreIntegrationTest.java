package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.washflow.application.idempotency.IdempotencyStore.Acquired;
import com.washflow.application.idempotency.IdempotencyStore.Claim;
import com.washflow.application.idempotency.IdempotencyStore.InProgress;
import com.washflow.application.idempotency.IdempotencyStore.Replay;
import java.time.Instant;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class JdbiIdempotencyStoreIntegrationTest {

  private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final Instant NOW = Instant.parse("2026-11-04T14:00:00Z");

  @Test
  void firstUseRunsThenRetriesReplayTheStoredResponse() {
    JdbiIdempotencyStore store = new JdbiIdempotencyStore(TestDatabase.prepare());

    assertInstanceOf(Acquired.class, claim(store, "venda-0001", "h1", NOW));
    assertEquals(new InProgress("h1"), claim(store, "venda-0001", "h1", NOW.plusSeconds(1)));

    store.complete(USER, "venda-0001", 200, "{\"id\":\"sale-1\"}");

    assertEquals(
        new Replay("h1", 200, "{\"id\":\"sale-1\"}"),
        claim(store, "venda-0001", "h1", NOW.plusSeconds(5)));
  }

  @Test
  void keysArePerUser() {
    JdbiIdempotencyStore store = new JdbiIdempotencyStore(TestDatabase.prepare());
    claim(store, "mesma-chave", "h1", NOW);

    assertInstanceOf(
        Acquired.class,
        store.claim(UUID.randomUUID(), "mesma-chave", null, "POST", "/api/sales", "h1", NOW));
  }

  @Test
  void aFailedRequestReleasesItsKeySoTheRetryRunsAgain() {
    JdbiIdempotencyStore store = new JdbiIdempotencyStore(TestDatabase.prepare());
    claim(store, "caixa-0001", "h1", NOW);

    store.release(USER, "caixa-0001");

    assertInstanceOf(Acquired.class, claim(store, "caixa-0001", "h1", NOW.plusSeconds(1)));
  }

  @Test
  void anAttemptThatNeverFinishedIsTakenOverAfterAWhile() {
    JdbiIdempotencyStore store = new JdbiIdempotencyStore(TestDatabase.prepare());
    claim(store, "entrada-0001", "h1", NOW); // server killed mid-request: never completed

    assertInstanceOf(InProgress.class, claim(store, "entrada-0001", "h1", NOW.plusSeconds(60)));
    assertInstanceOf(
        Acquired.class,
        claim(
            store,
            "entrada-0001",
            "h1",
            NOW.plus(JdbiIdempotencyStore.STALE_AFTER).plusSeconds(1)));
  }

  @Test
  void storedResponsesExpireAfterTheRetentionWindow() {
    Jdbi jdbi = TestDatabase.prepare();
    JdbiIdempotencyStore store = new JdbiIdempotencyStore(jdbi);
    claim(store, "venda-antiga", "h1", NOW);
    store.complete(USER, "venda-antiga", 200, "{}");

    Instant nextDay = NOW.plus(JdbiIdempotencyStore.RETENTION).plusSeconds(1);

    assertInstanceOf(Acquired.class, claim(store, "venda-antiga", "h2", nextDay));
    assertEquals(
        Integer.valueOf(1),
        jdbi.withHandle(
            h ->
                h.createQuery("SELECT COUNT(*) FROM chaves_idempotencia")
                    .mapTo(Integer.class)
                    .one()));
  }

  private static Claim claim(JdbiIdempotencyStore store, String key, String hash, Instant now) {
    return store.claim(USER, key, TestDatabase.ESTABLISHMENT_ID, "POST", "/api/sales", hash, now);
  }
}
