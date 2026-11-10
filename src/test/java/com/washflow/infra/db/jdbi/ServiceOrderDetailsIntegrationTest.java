package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

/** The yard, queue and washer screens show plate/customer/service from {@code Details}. */
class ServiceOrderDetailsIntegrationTest {

  private static final UUID DEMO_VEHICLE_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID DEMO_WASHER_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Test
  void ordersLoadedFromTheDatabaseCarryVehicleCustomerAndServiceDetails() {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    UUID orderId = insertOrder(jdbi, "AGUARDANDO_PATIO");

    List<ServiceOrder> queue = new YardJdbiRepository(jdbi).loadWaitingQueue();

    assertEquals(1, queue.size());
    ServiceOrder.Details details = queue.get(0).details();
    assertEquals("ABC1D23", details.vehiclePlate());
    assertEquals("Onix", details.vehicleModel());
    assertEquals("Prata", details.vehicleColor());
    assertEquals("Cliente Demo", details.customerName());
    assertEquals("Lavagem Completa", details.serviceName());
    assertNull(details.washerName());

    repository.updateStatus(
        orderId, ServiceOrderStatus.WAITING_IN_YARD, ServiceOrderStatus.WASHING, DEMO_WASHER_ID);

    assertEquals(
        "Lavador Demo",
        new YardJdbiRepository(jdbi).loadYardOverview().get(0).details().washerName());
    assertEquals(
        "ABC1D23",
        new YardJdbiRepository(jdbi)
            .loadWashingByWasher(DEMO_WASHER_ID)
            .get(0)
            .details()
            .vehiclePlate());
    assertEquals("ABC1D23", repository.loadById(orderId).orElseThrow().details().vehiclePlate());
  }

  @Test
  void customerOrderHistoryKeepsFilteringByTheVehicleOwner() {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    insertOrder(jdbi, "AGENDADO");

    List<ServiceOrder> ownOrders =
        repository.loadByCustomerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    List<ServiceOrder> otherOrders = repository.loadByCustomerId(UUID.randomUUID());

    assertEquals(1, ownOrders.size());
    assertEquals("Onix", ownOrders.get(0).details().vehicleModel());
    assertEquals(0, otherOrders.size());
  }

  private static UUID insertOrder(Jdbi jdbi, String status) {
    UUID orderId = UUID.randomUUID();
    Instant now = Instant.now();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status,
                       data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :tenant, :vehicleId, 1, :status, :now, :now, :now)
                    """)
                .bind("id", orderId)
                .bind("vehicleId", DEMO_VEHICLE_ID)
                .bind("status", status)
                .bind("now", now)
                .execute());
    return orderId;
  }

  private static Jdbi createJdbi() {
    DataSource dataSource = TestDatabase.newDatabase();
    return TestDatabase.prepare(dataSource);
  }
}
