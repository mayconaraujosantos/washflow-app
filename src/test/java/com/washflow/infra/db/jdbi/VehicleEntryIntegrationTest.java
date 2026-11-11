package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.usecases.schedule.DbListAgenda;
import com.washflow.data.usecases.vehicle.DbCreateVehicle;
import com.washflow.data.usecases.vehicle.DbListUnclaimedVehicles;
import com.washflow.data.usecases.vehicle.DbSearchVehicles;
import com.washflow.data.usecases.yard.DbRegisterVehicleEntry;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.entities.vehicle.VehicleLookup;
import com.washflow.domain.errors.vehicle.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.domain.usecases.vehicle.CreateVehicle;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class VehicleEntryIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");

  @Test
  void walkInEntryIsFoundBySearchAndShowsUpInTheYardAndAgenda() throws Exception {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    VehicleJdbiRepository vehicles = new VehicleJdbiRepository(jdbi);
    DbRegisterVehicleEntry register =
        new DbRegisterVehicleEntry(
            vehicles,
            vehicles,
            new UserJdbiRepository(jdbi),
            new ServicePriceJdbiRepository(jdbi),
            new AgendaJdbiRepository(jdbi),
            orders,
            orders,
            Clock.fixed(NOW, ZoneOffset.UTC),
            4);

    ServiceOrder order =
        register.register(
            new RegisterVehicleEntry.Params(
                "XYZ9A87",
                "HB20",
                "Branco",
                "João 50%_off",
                "(92) 99123-4567",
                1,
                null,
                NOW.plusSeconds(3600),
                "Cliente aguarda na loja"));

    assertEquals(ServiceOrderStatus.WAITING_IN_YARD, order.status());
    assertEquals("XYZ9A87", order.details().vehiclePlate());
    assertEquals("João 50%_off", order.details().customerName());
    assertEquals("92991234567", order.details().customerPhone());
    assertEquals("Lavagem Completa", order.details().serviceName());
    assertEquals(NOW.plusSeconds(3600), order.expectedDeliveryAt());
    assertEquals("Cliente aguarda na loja", order.notes());
    assertEquals(order.id(), new YardJdbiRepository(jdbi).loadWaitingQueue().get(0).id());

    DbSearchVehicles search = new DbSearchVehicles(vehicles);
    assertPlates(List.of("XYZ9A87"), search.search("xyz9"));
    assertPlates(List.of("XYZ9A87"), search.search("50%_"));
    assertPlates(List.of("XYZ9A87"), search.search("99123"));
    assertPlates(List.of("ABC1D23"), search.search("cliente demo"));
    assertTrue(search.search("50%x").isEmpty(), "LIKE wildcards must match literally");

    List<ServiceOrder> agenda =
        new DbListAgenda(new AgendaJdbiRepository(jdbi))
            .list(new ListAgenda.Params(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)));
    assertEquals(List.of(order.id()), agenda.stream().map(ServiceOrder::id).toList());
  }

  @Test
  void entryForAKnownPlateReusesTheVehicleAndItsOwner() throws Exception {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    VehicleJdbiRepository vehicles = new VehicleJdbiRepository(jdbi);
    DbRegisterVehicleEntry register =
        new DbRegisterVehicleEntry(
            vehicles,
            vehicles,
            new UserJdbiRepository(jdbi),
            new ServicePriceJdbiRepository(jdbi),
            new AgendaJdbiRepository(jdbi),
            orders,
            orders,
            Clock.fixed(NOW, ZoneOffset.UTC),
            4);

    ServiceOrder order =
        register.register(
            new RegisterVehicleEntry.Params(
                "abc1d23", null, null, null, null, 1, null, null, null));

    assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), order.vehicleId());
    assertEquals("Cliente Demo", order.details().customerName());
  }

  @Test
  void customerWithTheContactPhoneTakesOverTheCounterCar() throws Exception {
    Jdbi jdbi = createJdbi();
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);
    VehicleJdbiRepository vehicles = new VehicleJdbiRepository(jdbi);
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    new DbRegisterVehicleEntry(
            vehicles,
            vehicles,
            users,
            new ServicePriceJdbiRepository(jdbi),
            new AgendaJdbiRepository(jdbi),
            orders,
            orders,
            Clock.fixed(NOW, ZoneOffset.UTC),
            4)
        .register(
            new RegisterVehicleEntry.Params(
                "QWE4R56", "Onix", "Prata", "Ana", "92988887777", 1, null, null, null));

    UUID ana = UUID.randomUUID();
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    """
                    INSERT INTO usuarios (id, estabelecimento_id, nome, telefone, perfil)
                    VALUES (:id, :tenant, 'Ana', '92988887777', 'CLIENTE')
                    """)
                .bind("id", ana)
                .execute());

    DbListUnclaimedVehicles unclaimed = new DbListUnclaimedVehicles(users, vehicles);
    assertEquals(List.of("QWE4R56"), unclaimed.list(ana).stream().map(Vehicle::plate).toList());

    DbCreateVehicle create = new DbCreateVehicle(users, vehicles, vehicles, vehicles);
    Vehicle claimed = create.create(new CreateVehicle.Params(ana, "qwe-4r56", null, "Preto"));

    assertEquals(ana, claimed.customerId());
    assertEquals("Onix", claimed.model());
    assertEquals("Preto", claimed.color());
    Vehicle stored = vehicles.loadByPlate("QWE4R56").orElseThrow();
    assertEquals(ana, stored.customerId());
    assertEquals("Preto", stored.color());
    assertEquals(null, stored.contactPhone());
    assertTrue(unclaimed.list(ana).isEmpty());
    assertEquals(
        List.of(claimed.id()), vehicles.loadByCustomerId(ana).stream().map(Vehicle::id).toList());
    assertThrows(
        VehiclePlateAlreadyRegisteredError.class,
        () -> create.create(new CreateVehicle.Params(ana, "QWE4R56", null, null)));
  }

  private static void assertPlates(List<String> expected, List<VehicleLookup> actual) {
    assertEquals(expected, actual.stream().map(VehicleLookup::plate).toList());
  }

  private static Jdbi createJdbi() {
    DataSource dataSource = TestDatabase.newDatabase();
    return TestDatabase.prepare(dataSource);
  }
}
