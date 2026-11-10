package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceOrderCancellationIntegrationTest {

  private static final UUID VEHICLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID MANAGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final Instant SLOT = Instant.parse("2030-01-07T13:00:00Z");

  private Jdbi jdbi;
  private ServiceOrderJdbiRepository repository;

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    repository = new ServiceOrderJdbiRepository(jdbi);
  }

  @Test
  void cancelsScheduledOrWaitingOrdersAndRecordsWhoAndWhy() {
    UUID scheduled = order("AGENDADO");
    UUID waiting = order("AGUARDANDO_PATIO");

    assertTrue(repository.cancel(scheduled, MANAGER_ID, "NO_SHOW", null));
    assertTrue(repository.cancel(waiting, MANAGER_ID, "OTHER", "Cliente pediu para remarcar"));

    assertEquals(
        ServiceOrderStatus.CANCELLED, repository.loadById(scheduled).orElseThrow().status());
    Map<String, Object> row =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        """
                        SELECT cancelado_por, motivo_cancelamento, descricao_cancelamento,
                               cancelado_em
                        FROM atendimentos WHERE id = :id
                        """)
                    .bind("id", waiting)
                    .mapToMap()
                    .one());
    assertEquals(MANAGER_ID, row.get("cancelado_por"));
    assertEquals("OTHER", row.get("motivo_cancelamento"));
    assertEquals("Cliente pediu para remarcar", row.get("descricao_cancelamento"));
    assertTrue(row.get("cancelado_em") != null);
  }

  @Test
  void refusesOrdersAlreadyWashingFinishedOrCancelled() {
    for (String status : new String[] {"EM_LAVAGEM", "PAUSADA", "PRONTO", "FINALIZADO"}) {
      UUID id = order(status);

      assertFalse(repository.cancel(id, MANAGER_ID, "CUSTOMER_GAVE_UP", null), status);
      assertEquals(status, statusOf(id));
      jdbi.useHandle(h -> h.execute("DELETE FROM atendimentos WHERE id = ?", id));
    }
    UUID cancelled = order("AGENDADO");
    assertTrue(repository.cancel(cancelled, MANAGER_ID, "CUSTOMER_GAVE_UP", null));
    assertFalse(repository.cancel(cancelled, MANAGER_ID, "CUSTOMER_GAVE_UP", null));
  }

  @Test
  void cancelledOrdersFreeTheirSlotAndLeaveTheYard() {
    UUID id = order("AGENDADO");
    assertEquals(1, repository.countScheduledBetween(SLOT, SLOT.plusSeconds(3600)));

    repository.cancel(id, MANAGER_ID, "NO_SHOW", null);

    assertEquals(0, repository.countScheduledBetween(SLOT, SLOT.plusSeconds(3600)));
    assertTrue(repository.loadYardOverview().stream().map(ServiceOrder::id).noneMatch(id::equals));
  }

  private UUID order(String status) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status,
                       data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :tenant, :vehicleId, 1, :status, :slot, :now, :now)
                    """)
                .bind("id", id)
                .bind("vehicleId", VEHICLE_ID)
                .bind("status", status)
                .bind("slot", SLOT)
                .bind("now", now)
                .execute());
    return id;
  }

  private String statusOf(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT status FROM atendimentos WHERE id = :id")
                .bind("id", id)
                .mapTo(String.class)
                .one());
  }
}
