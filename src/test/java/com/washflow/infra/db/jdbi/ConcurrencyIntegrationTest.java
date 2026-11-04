package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.protocols.db.DuplicateKeyError;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.data.usecases.registervehicleentry.DbRegisterVehicleEntry;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.data.usecases.startserviceorder.DbStartServiceOrder;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.errors.VehicleAlreadyInYardError;
import com.washflow.domain.usecases.RegisterVehicleEntry;
import com.washflow.domain.usecases.StartServiceOrder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

/**
 * Real-world races against a real PostgreSQL: requests released at the same instant (a barrier)
 * from separate threads, each with its own connection - the double clicks, retries and two people
 * tapping at once that the single-threaded tests can't reproduce.
 */
class ConcurrencyIntegrationTest {

  private static final UUID WASHER_A = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z"); // Tue 10:00 Manaus

  @Test
  void twoWashersStartingTheSameCarAtOnceOnlyOneGetsIt() throws Exception {
    Jdbi jdbi = TestDatabase.prepare();
    UUID washerB = insertWasher(jdbi, "Lavador B", "11999992222");
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    UUID orderId = registerWalkIn(jdbi, "ABC1D23").id();
    DbStartServiceOrder start = new DbStartServiceOrder(orders, orders);

    List<Object> results =
        race(
            List.of(
                () -> start.start(new StartServiceOrder.Params(orderId, WASHER_A)),
                () -> start.start(new StartServiceOrder.Params(orderId, washerB))));

    List<ServiceOrder> winners =
        results.stream()
            .filter(ServiceOrder.class::isInstance)
            .map(ServiceOrder.class::cast)
            .toList();
    assertEquals(1, winners.size(), "exactly one washer starts the car: " + results);
    assertEquals(
        1,
        results.stream().filter(InvalidServiceOrderStatusError.class::isInstance).count(),
        "the other one gets a conflict (409): " + results);
    ServiceOrder stored = orders.loadById(orderId).orElseThrow();
    assertEquals(ServiceOrderStatus.WASHING, stored.status());
    assertEquals(winners.get(0).washerId(), stored.washerId(), "the winner keeps the car");
  }

  @Test
  void simultaneousBookingsNeverOverfillTheHour() throws Exception {
    Jdbi jdbi = TestDatabase.prepare(); // capacity 3 per hour
    Instant slot = Instant.parse("2026-11-04T14:00:00Z"); // Wed 10:00 Manaus
    DbRegisterVehicleEntry register = register(jdbi);
    String[] plates = {"AAA1A11", "BBB2B22", "CCC3C33", "DDD4D44", "EEE5E55", "FFF6F66", "GGG7G77"};
    List<Callable<Object>> bookings = new ArrayList<>();
    for (String plate : plates) {
      bookings.add(() -> register.register(entry(plate, slot)));
    }

    List<Object> results = race(bookings);

    long booked = results.stream().filter(ServiceOrder.class::isInstance).count();
    long refused = results.stream().filter(SlotFullyBookedError.class::isInstance).count();
    assertEquals(3, booked, "capacity is 3: " + results);
    assertEquals(plates.length - 3, refused, "the rest get 'slot full' (409): " + results);
    assertEquals(
        3,
        new ServiceOrderJdbiRepository(jdbi).countScheduledBetween(slot, slot.plusSeconds(3600)));
  }

  @Test
  void samePlateEnteredTwiceAtOnceCreatesOneOpenOrder() throws Exception {
    Jdbi jdbi = TestDatabase.prepare();
    DbRegisterVehicleEntry register = register(jdbi);

    List<Object> results =
        race(
            List.of(
                () -> register.register(entry("ABC1D23", null)),
                () -> register.register(entry("ABC1D23", null))));

    assertEquals(1, results.stream().filter(ServiceOrder.class::isInstance).count(), "" + results);
    assertEquals(
        1,
        results.stream().filter(VehicleAlreadyInYardError.class::isInstance).count(),
        "" + results);
    assertEquals(1, new ServiceOrderJdbiRepository(jdbi).loadYardOverview().size());
  }

  @Test
  void samePlateEnteredAgainWhileStillInTheYardIsRefusedButAllowedAfterFinalizing()
      throws Exception {
    Jdbi jdbi = TestDatabase.prepare();
    DbRegisterVehicleEntry register = register(jdbi);
    ServiceOrder first = register.register(entry("ABC1D23", null));

    VehicleAlreadyInYardError error =
        assertThrows(
            VehicleAlreadyInYardError.class, () -> register.register(entry("abc-1d23", null)));
    assertTrue(error.getMessage().contains("ABC1D23"), error.getMessage());

    // A future booking of the same car is fine while it's in the yard...
    ServiceOrder booking =
        register.register(entry("ABC1D23", Instant.parse("2026-11-05T14:00:00Z")));
    assertEquals(ServiceOrderStatus.SCHEDULED, booking.status());

    // ...and once the current wash is finalized, the car can come in again.
    jdbi.useHandle(
        h ->
            h.createUpdate("UPDATE atendimentos SET status = 'FINALIZADO' WHERE id = :id")
                .bind("id", first.id())
                .execute());
    assertEquals(
        ServiceOrderStatus.WAITING_IN_YARD, register.register(entry("ABC1D23", null)).status());
  }

  @Test
  void sameAccountCreatedTwiceAtOnceIsReportedAsDuplicateNotAServerError() throws Exception {
    Jdbi jdbi = TestDatabase.prepare();
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    Callable<Object> signUp =
        () ->
            users.create(
                new User(
                    UUID.randomUUID(), "Duplo", "92990000999", UserProfile.CUSTOMER, Instant.now()),
                "hash");

    List<Object> results = race(List.of(signUp, signUp));

    assertEquals(1, results.stream().filter(User.class::isInstance).count(), "" + results);
    assertInstanceOf(
        DuplicateKeyError.class,
        results.stream().filter(r -> !(r instanceof User)).findFirst().orElseThrow());
  }

  // ---------------------------------------------------------------- helpers

  /** Runs every task at the same instant on its own thread (same lava-jato); result or error. */
  private static List<Object> race(List<Callable<Object>> tasks) throws Exception {
    UUID tenant = TenantContext.require();
    CyclicBarrier barrier = new CyclicBarrier(tasks.size());
    ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    try {
      List<Future<Object>> futures = new ArrayList<>();
      for (Callable<Object> task : tasks) {
        futures.add(
            pool.submit(
                () -> {
                  TenantContext.set(tenant);
                  try {
                    barrier.await(10, TimeUnit.SECONDS);
                    return task.call();
                  } catch (Exception e) {
                    return e;
                  } finally {
                    TenantContext.clear();
                  }
                }));
      }
      List<Object> results = new ArrayList<>();
      for (Future<Object> future : futures) {
        results.add(future.get(30, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  private static DbRegisterVehicleEntry register(Jdbi jdbi) {
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    VehicleJdbiRepository vehicles = new VehicleJdbiRepository(jdbi);
    return new DbRegisterVehicleEntry(
        vehicles,
        vehicles,
        new UserJdbiRepository(jdbi),
        new ServicePriceJdbiRepository(jdbi),
        new SchedulingRules(orders, 3),
        orders,
        orders,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static ServiceOrder registerWalkIn(Jdbi jdbi, String plate) throws Exception {
    return register(jdbi).register(entry(plate, null));
  }

  private static RegisterVehicleEntry.Params entry(String plate, Instant scheduledAt) {
    return new RegisterVehicleEntry.Params(
        plate, "Carro", "Prata", "Cliente " + plate, null, 1, scheduledAt, null, null);
  }

  private static UUID insertWasher(Jdbi jdbi, String name, String phone) {
    UUID id = UUID.randomUUID();
    jdbi.useHandle(
        h ->
            h.createUpdate(
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
}
