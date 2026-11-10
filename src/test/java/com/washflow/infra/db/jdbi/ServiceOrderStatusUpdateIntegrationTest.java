package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class ServiceOrderStatusUpdateIntegrationTest {

  @Test
  void checkInWithNullWasherIdPreservesNullAndStartingAssignsWasher() {
    Jdbi jdbi = createJdbi();
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
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status, data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :tenant, :vehicleId, 1, 'AGENDADO', :now, :now, :now)
                    """)
                .bind("id", orderId)
                .bind("vehicleId", vehicleId)
                .bind("now", now)
                .execute());
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);

    ServiceOrder checkedIn =
        repository
            .updateStatus(
                orderId, ServiceOrderStatus.SCHEDULED, ServiceOrderStatus.WAITING_IN_YARD, null)
            .orElseThrow();
    ServiceOrder started =
        repository
            .updateStatus(
                orderId, ServiceOrderStatus.WAITING_IN_YARD, ServiceOrderStatus.WASHING, washerId)
            .orElseThrow();

    assertEquals(ServiceOrderStatus.WAITING_IN_YARD, checkedIn.status());
    assertNull(checkedIn.washerId());
    assertEquals(ServiceOrderStatus.WASHING, started.status());
    assertEquals(washerId, started.washerId());
  }

  private static Jdbi createJdbi() {
    DataSource dataSource = TestDatabase.newDatabase();
    return TestDatabase.prepare(dataSource);
  }
}
