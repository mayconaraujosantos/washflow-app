package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.washflow.data.tenancy.TenantContext;
import com.washflow.data.usecases.serviceorder.DbAssignWasher;
import com.washflow.data.usecases.serviceorder.DbStartServiceOrder;
import com.washflow.data.usecases.yard.DbGetWasherBoard;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotAssignableError;
import com.washflow.domain.errors.serviceorder.ServiceOrderReservedError;
import com.washflow.domain.errors.serviceorder.WasherBusyError;
import com.washflow.domain.errors.serviceorder.WasherNotAssignableError;
import com.washflow.domain.usecases.serviceorder.AssignWasher;
import com.washflow.domain.usecases.serviceorder.StartServiceOrder;
import com.washflow.domain.usecases.yard.GetWasherBoard;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WasherAssignmentIntegrationTest {

  private static final UUID ANA = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID CUSTOMER = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private Jdbi jdbi;
  private UUID bruno;
  private DbAssignWasher assign;
  private DbStartServiceOrder start;
  private YardJdbiRepository yard;

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    bruno = insertWasher("Bruno Lavador", "11999994444");
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    yard = new YardJdbiRepository(jdbi);
    assign = new DbAssignWasher(orders, orders, new UserJdbiRepository(jdbi));
    start = new DbStartServiceOrder(orders, orders, yard);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void aReservedCarOnlyShowsUpForItsWasherAndOnlyTheyStartIt() {
    UUID shared = waiting("AAA1A11", 10);
    UUID forBruno = waiting("BBB2B22", 5);

    ServiceOrder assigned = assign.assign(new AssignWasher.Params(forBruno, bruno));

    assertEquals(bruno, assigned.washerId());
    // Bruno sees his reservation first, then the shared queue; Ana only the shared car.
    assertEquals(List.of(forBruno, shared), ids(yard.loadWaitingQueue(bruno)));
    assertEquals(List.of(shared), ids(yard.loadWaitingQueue(ANA)));
    assertEquals(2, yard.loadWaitingQueue(null).size());

    assertThrows(
        ServiceOrderReservedError.class,
        () -> start.start(new StartServiceOrder.Params(forBruno, ANA)));
    start.start(new StartServiceOrder.Params(forBruno, bruno));
  }

  @Test
  void aWasherWashesOneCarAtATime() {
    UUID first = waiting("AAA1A11", 10);
    UUID second = waiting("BBB2B22", 5);
    start.start(new StartServiceOrder.Params(first, ANA));

    assertThrows(
        WasherBusyError.class, () -> start.start(new StartServiceOrder.Params(second, ANA)));
  }

  @Test
  void theCarGoesBackToTheSharedQueueAndCantBeReassignedOnceWashing() {
    UUID order = waiting("AAA1A11", 10);
    assign.assign(new AssignWasher.Params(order, bruno));

    assertNull(assign.assign(new AssignWasher.Params(order, null)).washerId());
    start.start(new StartServiceOrder.Params(order, ANA));

    assertThrows(
        ServiceOrderNotAssignableError.class,
        () -> assign.assign(new AssignWasher.Params(order, bruno)));
  }

  @Test
  void onlyActiveWashersOfTheLavaJatoCanBeChosen() {
    UUID order = waiting("AAA1A11", 10);

    assertThrows(
        WasherNotAssignableError.class,
        () -> assign.assign(new AssignWasher.Params(order, CUSTOMER)));
    jdbi.useHandle(
        handle -> handle.execute("UPDATE usuarios SET ativo = FALSE WHERE id = ?", bruno));
    assertThrows(
        WasherNotAssignableError.class, () -> assign.assign(new AssignWasher.Params(order, bruno)));
  }

  @Test
  void theBoardShowsWhoIsFreeWashingOrOnAPause() {
    UUID washing = waiting("AAA1A11", 30);
    UUID reserved = waiting("BBB2B22", 5);
    start.start(new StartServiceOrder.Params(washing, ANA));
    assign.assign(new AssignWasher.Params(reserved, bruno));

    List<GetWasherBoard.WasherStatus> board = new DbGetWasherBoard(yard).get();

    assertEquals(
        List.of("Bruno Lavador", "Lavador Demo"), board.stream().map(w -> w.name()).toList());
    GetWasherBoard.WasherStatus brunoStatus = board.get(0);
    assertEquals(GetWasherBoard.WasherStatus.FREE, brunoStatus.state());
    assertEquals(1, brunoStatus.reserved());
    GetWasherBoard.WasherStatus ana = board.get(1);
    assertEquals(GetWasherBoard.WasherStatus.WASHING, ana.state());
    assertEquals("AAA1A11", ana.current().vehiclePlate());

    Instant pausedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    jdbi.useHandle(
        handle -> {
          handle.execute("UPDATE atendimentos SET status = 'PAUSADA' WHERE id = ?", washing);
          handle
              .createUpdate(
                  """
                  INSERT INTO pausas_atendimento (id, atendimento_id, lavador_id, motivo,
                    iniciada_em)
                  VALUES (:id, :order, :washer, 'LUNCH', :at)
                  """)
              .bind("id", UUID.randomUUID())
              .bind("order", washing)
              .bind("washer", ANA)
              .bind("at", pausedAt)
              .execute();
        });

    GetWasherBoard.WasherStatus paused = new DbGetWasherBoard(yard).get().get(1);
    assertEquals(GetWasherBoard.WasherStatus.PAUSED, paused.state());
    assertEquals("LUNCH", paused.current().pauseReason());
    assertEquals(pausedAt, paused.current().pausedAt());
  }

  private static List<UUID> ids(List<ServiceOrder> orders) {
    return orders.stream().map(ServiceOrder::id).toList();
  }

  private UUID insertWasher(String name, String phone) {
    UUID id = UUID.randomUUID();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO usuarios (id, estabelecimento_id, nome, telefone, perfil)
                    VALUES (:id, :tenant, :name, :phone, 'LAVADOR')
                    """)
                .bind("id", id)
                .bind("name", name)
                .bind("phone", phone)
                .execute());
    return id;
  }

  /** A walk-in car waiting in the yard for {@code minutes}. */
  private UUID waiting(String plate, long minutes) {
    UUID vehicle = UUID.randomUUID();
    UUID order = UUID.randomUUID();
    Instant arrived = Instant.now().minus(minutes, ChronoUnit.MINUTES);
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(
                  "INSERT INTO veiculos (id, estabelecimento_id, placa) VALUES (:id, :tenant, :plate)")
              .bind("id", vehicle)
              .bind("plate", plate)
              .execute();
          handle
              .createUpdate(
                  """
                  INSERT INTO atendimentos
                    (id, estabelecimento_id, veiculo_id, servico_preco_id, status,
                     data_agendamento, criado_em, atualizado_em)
                  VALUES (:id, :tenant, :vehicle, 1, 'AGUARDANDO_PATIO', :at, :at, :at)
                  """)
              .bind("id", order)
              .bind("vehicle", vehicle)
              .bind("at", arrived)
              .execute();
        });
    return order;
  }
}
