package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.AppConfig;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.data.usecases.establishment.DbManageEstablishment;
import com.washflow.data.usecases.establishment.DbManagePlatform;
import com.washflow.data.usecases.establishment.DbSetupEstablishment;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.data.usecases.user.DbAuthenticateUser;
import com.washflow.data.usecases.user.DbUpdateUserEmail;
import com.washflow.domain.entities.establishment.Establishment;
import com.washflow.domain.entities.establishment.EstablishmentSummary;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.common.TenantRequiredError;
import com.washflow.domain.errors.establishment.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.establishment.EstablishmentSlugTakenError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.domain.usecases.user.AuthenticateUser;
import com.washflow.infra.db.jdbi.establishment.EstablishmentJdbiRepository;
import com.washflow.infra.db.jdbi.establishment.PlatformJdbiRepository;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Platform setup, lava-jato creation and the isolation between lava-jatos. */
class EstablishmentIntegrationTest {

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void setupCreatesThePlatformAdministratorOnce() throws Exception {
    Jdbi jdbi = createJdbi();
    PlatformJdbiRepository platform = new PlatformJdbiRepository(jdbi);
    DbSetupEstablishment setup =
        new DbSetupEstablishment(
            platform, new BCryptPasswordHasher(), "", false, Clock.systemUTC());

    assertFalse(setup.status().configured());
    User admin = setup.setup(params("Dona Maria", "Maria@Washflow.com", "senha-forte-1"));

    assertTrue(setup.status().configured());
    assertEquals(UserProfile.ADMIN, admin.profile());
    assertEquals("maria@washflow.com", admin.email());
    assertThrows(
        EstablishmentAlreadyConfiguredError.class,
        () -> setup.setup(params("Outro", "x@y.com", "senha-forte-2")));

    DbManagePlatform manage = platform(jdbi);
    assertEquals(admin.id(), manage.signIn("MARIA@washflow.com", "senha-forte-1").id());
    assertThrows(
        InvalidCredentialsError.class, () -> manage.signIn("maria@washflow.com", "errada-1"));
  }

  @Test
  void eachLavaJatoSeesOnlyItsOwnData() throws Exception {
    Jdbi jdbi = createJdbi();
    DbManagePlatform platform = platform(jdbi);
    EstablishmentSummary centro =
        platform.create(newEstablishment("Lava Rápido Centro", "ana@centro.com"));
    EstablishmentSummary bairro =
        platform.create(newEstablishment("Lava Bairro", "bia@bairro.com"));
    assertEquals("lava-rapido-centro", centro.slug());
    assertEquals("ana@centro.com", centro.managerEmail());

    ServicePriceJdbiRepository services = new ServicePriceJdbiRepository(jdbi);
    VehicleJdbiRepository vehicles = new VehicleJdbiRepository(jdbi);
    ServiceOrderJdbiRepository orders = new ServiceOrderJdbiRepository(jdbi);

    UUID centroVehicle = UUID.randomUUID();
    UUID centroOrder = UUID.randomUUID();
    TenantContext.set(centro.id());
    assertEquals(8, services.loadAll().size(), "every lava-jato starts with the starter catalog");
    int centroService = services.loadAll().get(0).id();
    vehicles.create(new Vehicle(centroVehicle, null, "ABC1D23", "Onix", "Prata", "Ana", null));
    Instant now = Instant.now();
    orders.create(
        new ServiceOrder(
            centroOrder,
            centroVehicle,
            centroService,
            null,
            ServiceOrderStatus.WAITING_IN_YARD,
            now,
            now,
            now,
            null));
    assertEquals(1, new YardJdbiRepository(jdbi).loadYardOverview().size());

    TenantContext.set(bairro.id());
    // The same plate can exist in another lava-jato; nothing of "Centro" is visible here.
    vehicles.create(new Vehicle(UUID.randomUUID(), null, "ABC1D23", "Gol", "Preto", "Bia", null));
    assertTrue(vehicles.loadById(centroVehicle).isEmpty());
    assertTrue(orders.loadById(centroOrder).isEmpty());
    assertTrue(new YardJdbiRepository(jdbi).loadYardOverview().isEmpty());
    assertTrue(services.loadById(centroService).isEmpty());
    assertEquals(1, vehicles.search("ABC", "", "", 10).size());

    // Logins by e-mail are global and say which lava-jato the manager belongs to.
    TenantContext.clear();
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    assertTrue(users.loadByEmail("ana@centro.com").isPresent());
    assertThrows(TenantRequiredError.class, services::loadAll);

    // Suspending a lava-jato blocks its staff logins and hides it from the customer picker.
    platform.setActive(centro.id(), false);
    assertTrue(users.loadByEmail("ana@centro.com").isEmpty());
    assertEquals(1, platform.search("lava").size());
    assertEquals("lava-bairro", platform.search("").get(0).slug());
  }

  @Test
  void rejectsDuplicateAddressesAndE_mails() throws Exception {
    Jdbi jdbi = createJdbi();
    DbManagePlatform platform = platform(jdbi);
    platform.create(newEstablishment("Lava Centro", "ana@centro.com"));

    assertThrows(
        EstablishmentSlugTakenError.class,
        () -> platform.create(newEstablishment("Lava Centro", "outra@centro.com")));
    assertThrows(
        EmailAlreadyRegisteredError.class,
        () -> platform.create(newEstablishment("Lava Norte", "ana@centro.com")));
  }

  @Test
  void managerLogsInByE_mailIntoTheirLavaJato() throws Exception {
    Jdbi jdbi = createJdbi();
    EstablishmentSummary centro =
        platform(jdbi).create(newEstablishment("Lava Centro", "ana@centro.com"));
    TenantContext.clear();
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    DbAuthenticateUser auth =
        new DbAuthenticateUser(
            users,
            users,
            users,
            users,
            new BCryptPasswordHasher(),
            AppConfig.load(Map.of("APP_ENV", "production-like"), Map.of()),
            users);

    User manager =
        auth.authenticate(
            new AuthenticateUser.Params(
                null, null, "senha-do-gerente", UserProfile.MANAGER, "ANA@centro.com"));

    assertEquals(UserProfile.MANAGER, manager.profile());
    assertEquals(centro.managerName(), manager.name());
    assertThrows(
        InvalidCredentialsError.class,
        () ->
            auth.authenticate(
                new AuthenticateUser.Params(
                    null, null, "errada-123", UserProfile.MANAGER, "ana@centro.com")));
  }

  @Test
  void settingsDriveTheSchedulingRules() throws Exception {
    Jdbi jdbi = TestDatabase.prepare(migratedDataSource());
    EstablishmentJdbiRepository establishments = new EstablishmentJdbiRepository(jdbi);
    DbManageEstablishment manage = new DbManageEstablishment(establishments);
    assertEquals("Lava Teste", manage.get().name());

    Establishment saved = manage.update(new ManageEstablishment.Params("Lava 24h", 6, 22, true, 7));

    assertEquals(22, saved.closingHour());
    SchedulingRules rules =
        new SchedulingRules(
            new AgendaJdbiRepository(jdbi),
            () -> {
              Establishment e = establishments.load();
              return new SchedulingRules.Hours(
                  e.openingHour(), e.closingHour(), e.openOnSunday(), e.maxVehiclesPerHour());
            });
    // Sunday 2026-11-08 10:00 Manaus: allowed now that it opens on Sundays.
    rules.checkBusinessHours(Instant.parse("2026-11-08T14:00:00Z"));
    // 05:00 Manaus is before the new opening hour.
    assertThrows(
        OutsideBusinessHoursError.class,
        () -> rules.checkBusinessHours(Instant.parse("2026-11-09T09:00:00Z")));
  }

  @Test
  void managersCanSetUniqueLoginE_mails() throws Exception {
    Jdbi jdbi = TestDatabase.prepare(migratedDataSource());
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    DbUpdateUserEmail update = new DbUpdateUserEmail(users, users, users);
    UUID demoManager = UUID.fromString("44444444-4444-4444-4444-444444444444");
    UUID demoWasher = UUID.fromString("33333333-3333-3333-3333-333333333333");

    assertEquals(
        "gerente@lavajato.com", update.update(demoManager, "Gerente@LavaJato.com").email());
    assertThrows(
        EmailAlreadyRegisteredError.class, () -> update.update(demoWasher, "gerente@lavajato.com"));
    assertNull(update.update(demoManager, " ").email());
  }

  private static SetupEstablishment.Params params(String name, String email, String password) {
    return new SetupEstablishment.Params(null, name, email, password);
  }

  private static ManagePlatform.NewEstablishment newEstablishment(String name, String email) {
    return new ManagePlatform.NewEstablishment(
        name, null, 8, 18, false, 3, "Gerente " + name, email, null, "senha-do-gerente");
  }

  private static DbManagePlatform platform(Jdbi jdbi) {
    return new DbManagePlatform(
        new PlatformJdbiRepository(jdbi), new BCryptPasswordHasher(), Clock.systemUTC());
  }

  /** Empty platform (no test lava-jato): what a fresh installation looks like. */
  private static Jdbi createJdbi() {
    Jdbi jdbi = Jdbi.create(migratedDataSource());
    jdbi.getConfig(org.jdbi.v3.core.statement.SqlStatements.class)
        .addCustomizer(new com.washflow.infra.db.tenancy.TenantBindingCustomizer());
    return jdbi;
  }

  private static DataSource migratedDataSource() {
    return TestDatabase.newDatabase();
  }
}
