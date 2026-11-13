package com.washflow.infra.db.jdbi.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.protocols.db.whatsapp.NotificationRepository;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.infra.db.jdbi.TestDatabase;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationJdbiRepositoryIntegrationTest {

  private static final UUID VEHICLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private Jdbi jdbi;
  private NotificationJdbiRepository repository;
  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    repository = new NotificationJdbiRepository(jdbi);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void findsTheCustomerOfTheOrder() {
    UUID order = order("PRONTO", now, now.minus(Duration.ofHours(3)));

    NotificationRepository.Recipient recipient = repository.recipientOf(order).orElseThrow();

    assertEquals("11999990000", recipient.phone());
    assertEquals("Cliente Demo", recipient.customerName());
    assertEquals("ABC1D23", recipient.vehiclePlate());
    assertEquals("Lava Teste", recipient.establishmentName());
  }

  @Test
  void remindsOnlyScheduledOrdersBookedAheadAndStartingWithinTheWindow() {
    UUID due = order("AGENDADO", now.plus(Duration.ofMinutes(40)), now.minus(Duration.ofDays(1)));
    order("AGENDADO", now.plus(Duration.ofHours(3)), now.minus(Duration.ofDays(1)));
    order("AGENDADO", now.plus(Duration.ofMinutes(40)), now.minus(Duration.ofMinutes(30)));
    order("CANCELADO", now.plus(Duration.ofMinutes(40)), now.minus(Duration.ofDays(1)));

    List<NotificationRepository.DueReminder> reminders =
        repository.dueReminders(now, now.plus(Duration.ofHours(1)));

    assertEquals(
        List.of(new NotificationRepository.DueReminder(due, TestDatabase.ESTABLISHMENT_ID)),
        reminders);
  }

  @Test
  void aReminderIsClaimedOnce() {
    UUID due = order("AGENDADO", now.plus(Duration.ofMinutes(40)), now.minus(Duration.ofDays(1)));

    assertTrue(repository.markReminded(due, now));
    assertFalse(repository.markReminded(due, now));
    assertTrue(repository.dueReminders(now, now.plus(Duration.ofHours(1))).isEmpty());
  }

  private UUID order(String status, Instant scheduledAt, Instant createdAt) {
    UUID id = UUID.randomUUID();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status,
                       data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :tenant, :vehicleId, 1, :status, :scheduledAt, :createdAt,
                            :createdAt)
                    """)
                .bind("id", id)
                .bind("vehicleId", VEHICLE_ID)
                .bind("status", status)
                .bind("scheduledAt", scheduledAt)
                .bind("createdAt", createdAt)
                .execute());
    return id;
  }
}
