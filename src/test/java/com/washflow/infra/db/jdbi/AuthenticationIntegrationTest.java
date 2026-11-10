package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.user.DbAuthenticateUser;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.user.AuthenticateUser;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class AuthenticationIntegrationTest {

  @Test
  void persistsBCryptCredentialsAndRejectsWrongPasswords() throws Exception {
    DataSource dataSource = TestDatabase.newDatabase();
    Jdbi jdbi = TestDatabase.prepare(dataSource);
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    DbAuthenticateUser authenticate =
        new DbAuthenticateUser(
            repository,
            repository,
            repository,
            repository,
            new BCryptPasswordHasher(),
            AppConfig.load(Map.of("APP_ENV", "development"), Map.of()));

    AuthenticateUser.Params registration =
        new AuthenticateUser.Params(
            "11912345678", "Cliente Teste", "senha-segura", UserProfile.CUSTOMER);
    authenticate.authenticate(registration);

    var signedInUser =
        authenticate.authenticate(
            new AuthenticateUser.Params(
                registration.phone(),
                "Nome ignorado no login",
                registration.password(),
                UserProfile.CUSTOMER));

    assertEquals(registration.phone(), signedInUser.phone());
    assertThrows(
        InvalidCredentialsError.class,
        () ->
            authenticate.authenticate(
                new AuthenticateUser.Params(
                    registration.phone(),
                    registration.name(),
                    "senha-incorreta",
                    UserProfile.CUSTOMER)));

    String washerPassword = "senha-lavador-segura";
    User provisionedWasher =
        repository.create(
            new User(
                UUID.randomUUID(),
                "Lavador Provisionado",
                "11912345679",
                UserProfile.WASHER,
                Instant.now()),
            new BCryptPasswordHasher().hash(washerPassword));
    assertEquals(
        UserProfile.WASHER,
        authenticate
            .authenticate(
                new AuthenticateUser.Params(
                    provisionedWasher.phone(),
                    provisionedWasher.name(),
                    washerPassword,
                    UserProfile.WASHER))
            .profile());

    AuthenticateUser.Params managerLogin =
        new AuthenticateUser.Params(
            "11999993333", "Gerente Demo", "senha-gerente-local", UserProfile.MANAGER);
    var manager = authenticate.authenticate(managerLogin);

    assertEquals(UserProfile.MANAGER, manager.profile());
    assertThrows(
        InvalidCredentialsError.class,
        () ->
            authenticate.authenticate(
                new AuthenticateUser.Params(
                    managerLogin.phone(),
                    managerLogin.name(),
                    "senha-incorreta",
                    UserProfile.MANAGER)));

    repository.updateActive(manager.id(), false);
    assertFalse(repository.loadById(manager.id()).orElseThrow().active());
    assertThrows(InvalidCredentialsError.class, () -> authenticate.authenticate(managerLogin));
  }
}
