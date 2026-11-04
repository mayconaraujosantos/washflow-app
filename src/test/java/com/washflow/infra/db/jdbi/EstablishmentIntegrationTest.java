package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.authenticateuser.DbAuthenticateUser;
import com.washflow.data.usecases.establishment.DbManageEstablishment;
import com.washflow.data.usecases.establishment.DbSetupEstablishment;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.data.usecases.updateuseremail.DbUpdateUserEmail;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.usecases.AuthenticateUser;
import com.washflow.domain.usecases.ManageEstablishment;
import com.washflow.domain.usecases.SetupEstablishment;
import com.washflow.infra.security.BCryptPasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class EstablishmentIntegrationTest {

  @Test
  void setupCreatesTheOwnerWhoThenLogsInByE_mail() throws Exception {
    Jdbi jdbi = createJdbi();
    EstablishmentJdbiRepository establishments = new EstablishmentJdbiRepository(jdbi);
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    DbSetupEstablishment setup =
        new DbSetupEstablishment(
            establishments, users, users, new BCryptPasswordHasher(), "", false, Clock.systemUTC());

    assertFalse(setup.status().configured());
    User owner =
        setup.setup(
            new SetupEstablishment.Params(
                null,
                "Lava Rápido Centro",
                7,
                19,
                true,
                5,
                "Dona Maria",
                "Maria@LavaJato.com",
                null,
                "senha-forte-1"));

    assertTrue(setup.status().configured());
    assertEquals("maria@lavajato.com", owner.email());
    assertThrows(
        EstablishmentAlreadyConfiguredError.class,
        () ->
            setup.setup(
                new SetupEstablishment.Params(
                    null, "Outro", 8, 18, false, 3, "X", "x@y.com", null, "senha-forte-2")));

    DbAuthenticateUser auth =
        new DbAuthenticateUser(
            users,
            users,
            users,
            users,
            new BCryptPasswordHasher(),
            AppConfig.load(Map.of("APP_ENV", "production-like"), Map.of()),
            users);
    User loggedIn =
        auth.authenticate(
            new AuthenticateUser.Params(
                null, null, "senha-forte-1", UserProfile.MANAGER, "MARIA@lavajato.com"));
    assertEquals(owner.id(), loggedIn.id());
    assertThrows(
        InvalidCredentialsError.class,
        () ->
            auth.authenticate(
                new AuthenticateUser.Params(
                    null, null, "errada-123", UserProfile.MANAGER, "maria@lavajato.com")));
    assertThrows(
        InvalidCredentialsError.class,
        () ->
            auth.authenticate(
                new AuthenticateUser.Params(
                    null, null, "senha-forte-1", UserProfile.MANAGER, "ninguem@x.com")));
  }

  @Test
  void settingsDriveTheSchedulingRules() throws Exception {
    Jdbi jdbi = createJdbi();
    EstablishmentJdbiRepository establishments = new EstablishmentJdbiRepository(jdbi);
    DbManageEstablishment manage = new DbManageEstablishment(establishments);
    assertNull(manage.get().configuredAt());

    Establishment saved = manage.update(new ManageEstablishment.Params("Lava 24h", 6, 22, true, 7));

    assertEquals(22, saved.closingHour());
    assertTrue(saved.isConfigured(), "saving the settings closes the setup wizard");
    SchedulingRules rules =
        new SchedulingRules(
            new ServiceOrderJdbiRepository(jdbi),
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
    Jdbi jdbi = createJdbi();
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
