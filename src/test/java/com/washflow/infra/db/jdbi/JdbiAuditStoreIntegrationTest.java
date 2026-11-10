package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.audit.AuditStore;
import com.washflow.application.audit.AuditStore.Entry;
import com.washflow.data.tenancy.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class JdbiAuditStoreIntegrationTest {

  private static final UUID MANAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Test
  void entriesAreListedNewestFirstWithTheAuthorsName() {
    Jdbi jdbi = TestDatabase.prepare();
    AuditStore store = new JdbiAuditStore(jdbi);

    store.record(entry(TestDatabase.ESTABLISHMENT_ID, "2026-11-10T12:00:00Z", "/api/services"));
    store.record(
        entry(TestDatabase.ESTABLISHMENT_ID, "2026-11-10T15:00:00Z", "/api/cash-transactions/x"));

    List<AuditStore.Row> rows =
        store.list(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"));

    assertEquals(2, rows.size());
    assertEquals("/api/cash-transactions/x", rows.get(0).path());
    assertEquals("Gerente Demo", rows.get(0).userName());
    assertEquals("MANAGER", rows.get(0).profile());
    assertEquals("{\"price\":70}", rows.get(1).details());
  }

  @Test
  void theDateRangeUsesManausTime() {
    Jdbi jdbi = TestDatabase.prepare();
    AuditStore store = new JdbiAuditStore(jdbi);
    // 02:00Z on the 11th is still 22:00 on the 10th in Manaus.
    store.record(entry(TestDatabase.ESTABLISHMENT_ID, "2026-11-11T02:00:00Z", "/api/sales"));

    assertEquals(
        1, store.list(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10")).size());
    assertEquals(
        0, store.list(LocalDate.parse("2026-11-11"), LocalDate.parse("2026-11-11")).size());
  }

  @Test
  void anotherLavaJatoSeesNothing() {
    Jdbi jdbi = TestDatabase.prepare();
    AuditStore store = new JdbiAuditStore(jdbi);
    store.record(entry(TestDatabase.ESTABLISHMENT_ID, "2026-11-10T12:00:00Z", "/api/sales"));

    UUID other = UUID.randomUUID();
    jdbi.useHandle(
        handle ->
            handle.execute(
                "INSERT INTO estabelecimentos (id, slug, nome, hora_abertura, hora_fechamento,"
                    + " abre_domingo, max_veiculos_hora, ativo)"
                    + " VALUES (?, 'outro', 'Outro', 8, 18, FALSE, 3, TRUE)",
                other));

    List<AuditStore.Row> rows =
        TenantContext.runAs(
            other, () -> store.list(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10")));

    assertTrue(rows.isEmpty());
  }

  @Test
  void theTrailCannotBeEditedOrErased() {
    Jdbi jdbi = TestDatabase.prepare();
    new JdbiAuditStore(jdbi)
        .record(entry(TestDatabase.ESTABLISHMENT_ID, "2026-11-10T12:00:00Z", "/api/sales"));

    assertThrows(
        Exception.class,
        () -> jdbi.useHandle(handle -> handle.execute("UPDATE auditoria SET status = 500")));
    assertThrows(
        Exception.class, () -> jdbi.useHandle(handle -> handle.execute("DELETE FROM auditoria")));
  }

  @Test
  void platformAdminActionsAreRecordedWithoutALavaJato() {
    Jdbi jdbi = TestDatabase.prepare();
    new JdbiAuditStore(jdbi).record(entry(null, "2026-11-10T12:00:00Z", "/api/admin/x"));

    int count =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT COUNT(*) FROM auditoria WHERE estabelecimento_id IS NULL")
                    .mapTo(Integer.class)
                    .one());
    assertEquals(1, count);
  }

  private static Entry entry(UUID establishment, String at, String path) {
    return new Entry(
        establishment,
        MANAGER,
        "MANAGER",
        "PUT",
        path,
        path,
        200,
        "{\"price\":70}",
        Instant.parse(at));
  }
}
