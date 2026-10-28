package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.entities.WasherCommission;
import com.washflow.domain.errors.UserHasActiveWashesError;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class OperationsWorkflowIntegrationTest {

  @Test
  void washerCanPauseAndResumeAndManagerReportIncludesCompletedWork() {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    UUID orderId = UUID.randomUUID();
    UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    UUID washerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    Instant now = Instant.now();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, veiculo_id, servico_preco_id, lavador_id, status,
                       data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :vehicleId, 1, :washerId, 'AGUARDANDO_PATIO', :now, :now, :now)
                    """)
                .bind("id", orderId)
                .bind("vehicleId", vehicleId)
                .bind("washerId", washerId)
                .bind("now", now)
                .execute());

    repository.updateStatus(orderId, ServiceOrderStatus.WASHING, washerId);
    assertThrows(
        UserHasActiveWashesError.class,
        () -> new UserJdbiRepository(jdbi).updateProfile(washerId, UserProfile.MANAGER));
    assertThrows(
        UserHasActiveWashesError.class,
        () -> new UserJdbiRepository(jdbi).updateActive(washerId, false));
    assertTrue(repository.pauseWashing(orderId, washerId, "LUNCH", null));
    ServiceOrder paused = repository.loadById(orderId).orElseThrow();
    assertEquals(ServiceOrderStatus.PAUSED, paused.status());
    assertEquals("LUNCH", paused.pauseReason());
    assertFalse(repository.pauseWashing(orderId, washerId, "RESTROOM", null));
    assertTrue(repository.resumeWashing(orderId, washerId));
    ServiceOrder resumed = repository.loadById(orderId).orElseThrow();
    assertEquals(ServiceOrderStatus.WASHING, resumed.status());
    assertNull(resumed.pauseReason());
    assertFalse(repository.resumeWashing(orderId, washerId));

    ServiceOrder ready = repository.updateStatus(orderId, ServiceOrderStatus.READY, washerId);
    repository.finalizeSale(
        ready,
        new ServicePayment(
            UUID.randomUUID(), orderId, PaymentMethod.PIX, new BigDecimal("60.00"), Instant.now()),
        new WasherCommission(
            UUID.randomUUID(), orderId, washerId, new BigDecimal("20.00"), Instant.now()));
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(
                  """
                  UPDATE atendimentos
                  SET lavagem_iniciada_em = :startedAt, lavagem_concluida_em = :completedAt
                  WHERE id = :id
                  """)
              .bind("startedAt", now.minusSeconds(3600))
              .bind("completedAt", now)
              .bind("id", orderId)
              .execute();
          handle
              .createUpdate(
                  """
                  UPDATE pausas_atendimento
                  SET iniciada_em = :startedAt, retomada_em = :resumedAt
                  WHERE atendimento_id = :id
                  """)
              .bind("startedAt", now.minusSeconds(1800))
              .bind("resumedAt", now.minusSeconds(1200))
              .bind("id", orderId)
              .execute();
        });

    LocalDate reportDate = LocalDate.now(ZoneId.of("America/Manaus"));
    OperationsReport report = repository.loadOperationsReport(reportDate, reportDate);

    assertEquals(1, report.finalizedOrders());
    assertEquals(new BigDecimal("60.00"), report.revenue());
    assertEquals(new BigDecimal("20.00"), report.commissions());
    assertEquals(50L, report.averageWashingMinutes());
    assertEquals(1, report.services().size());
    assertEquals("Lavagem Completa", report.services().getFirst().serviceName());
    assertEquals("PIX", report.paymentMethods().getFirst().method());
    assertEquals("LUNCH", report.pauses().getFirst().reason());
    assertEquals(1, report.pauses().getFirst().count());
  }

  private static Jdbi createJdbi() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:"
            + UUID.randomUUID().toString().replace("-", "")
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration-h2")
        .load()
        .migrate();
    return Jdbi.create(dataSource);
  }
}
