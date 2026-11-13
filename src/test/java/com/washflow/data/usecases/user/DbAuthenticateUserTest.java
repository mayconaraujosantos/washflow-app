package com.washflow.data.usecases.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.shared.DuplicateKeyError;
import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.LoadUserPasswordHashRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.errors.user.WasherSelfRegistrationNotAllowedError;
import com.washflow.domain.usecases.user.AuthenticateUser;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbScheduleServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbAuthenticateUserTest {

  @Mock private LoadUserByPhoneRepository loadUserByPhoneRepository;
  @Mock private CreateUserRepository createUserRepository;
  @Mock private LoadUserPasswordHashRepository loadUserPasswordHashRepository;
  @Mock private SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  @Mock private PasswordHasher passwordHasher;

  private DbAuthenticateUser sut;
  private AuthenticateUser.Params params;

  @BeforeEach
  void setUp() {
    AppConfig appConfig = AppConfig.load(Map.of("APP_ENV", "development"), Map.of());
    sut =
        new DbAuthenticateUser(
            loadUserByPhoneRepository,
            createUserRepository,
            loadUserPasswordHashRepository,
            saveUserPasswordHashRepository,
            passwordHasher,
            appConfig);
    params =
        new AuthenticateUser.Params(
            "11999990000", "Cliente Demo", "senha-segura", UserProfile.CUSTOMER);
  }

  @Test
  void registersNewUserWithExpectedProfileWhenPhoneNotYetRegistered() throws Exception {
    when(loadUserByPhoneRepository.loadByPhone(params.phone())).thenReturn(Optional.empty());
    when(passwordHasher.hash(anyString())).thenReturn("bcrypt-hash");
    when(createUserRepository.create(any(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User user = sut.authenticate(params);

    assertEquals(params.phone(), user.phone());
    assertEquals(params.name(), user.name());
    assertEquals(UserProfile.CUSTOMER, user.profile());
    verify(createUserRepository).create(any(), eq("bcrypt-hash"));
  }

  @Test
  void signUpSentTwiceAtOnceSignsTheLoserInToTheAccountJustCreated() throws Exception {
    // Both requests saw no account; the other one inserted first and this insert hit the
    // unique phone index.
    User createdByOther =
        new User(
            UUID.randomUUID(), "Cliente Demo", params.phone(), UserProfile.CUSTOMER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(createdByOther));
    when(passwordHasher.hash(anyString())).thenReturn("bcrypt-hash");
    when(createUserRepository.create(any(), anyString()))
        .thenThrow(new DuplicateKeyError("usuarios_telefone", null));
    when(loadUserPasswordHashRepository.loadPasswordHash(createdByOther.id()))
        .thenReturn(Optional.of("other-hash"));
    when(passwordHasher.matches(params.password(), "other-hash")).thenReturn(true);

    assertEquals(createdByOther, sut.authenticate(params));
  }

  @Test
  void signUpRaceWithAnotherPasswordIsRefusedAsWrongCredentials() {
    User createdByOther =
        new User(
            UUID.randomUUID(), "Outra Pessoa", params.phone(), UserProfile.CUSTOMER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(createdByOther));
    when(passwordHasher.hash(anyString())).thenReturn("bcrypt-hash");
    when(createUserRepository.create(any(), anyString()))
        .thenThrow(new DuplicateKeyError("usuarios_telefone", null));
    when(loadUserPasswordHashRepository.loadPasswordHash(createdByOther.id()))
        .thenReturn(Optional.of("other-hash"));
    when(passwordHasher.matches(params.password(), "other-hash")).thenReturn(false);

    assertThrows(InvalidCredentialsError.class, () -> sut.authenticate(params));
  }

  @Test
  void requiresANameOnlyWhenCreatingTheCustomerAccount() {
    when(loadUserByPhoneRepository.loadByPhone(params.phone())).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.authenticate(
                new AuthenticateUser.Params(
                    params.phone(), " ", "senha-segura", UserProfile.CUSTOMER)));
    verifyNoInteractions(createUserRepository);
  }

  @Test
  void returnsExistingUserWhenProfileMatches() throws Exception {
    User existingUser =
        new User(
            UUID.randomUUID(), "Cliente Demo", params.phone(), UserProfile.CUSTOMER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.of(existingUser));
    when(loadUserPasswordHashRepository.loadPasswordHash(existingUser.id()))
        .thenReturn(Optional.of("existing-hash"));
    when(passwordHasher.matches(params.password(), "existing-hash")).thenReturn(true);

    User user = sut.authenticate(params);

    assertEquals(existingUser, user);
    verify(createUserRepository, never()).create(any(), anyString());
  }

  @Test
  void throwsUserProfileMismatchWhenPhoneIsRegisteredUnderAnotherProfile() {
    User existingWasher =
        new User(
            UUID.randomUUID(), "Lavador Demo", params.phone(), UserProfile.WASHER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.of(existingWasher));

    assertThrows(UserProfileMismatchError.class, () -> sut.authenticate(params));
    verify(createUserRepository, never()).create(any(), anyString());
  }

  @Test
  void throwsManagerSelfRegistrationNotAllowedWhenPhoneNotRegisteredAndExpectingManager() {
    var managerParams =
        new AuthenticateUser.Params(
            "11999992222", "Gerente Novo", "senha-segura", UserProfile.MANAGER);
    when(loadUserByPhoneRepository.loadByPhone(managerParams.phone())).thenReturn(Optional.empty());

    assertThrows(
        ManagerSelfRegistrationNotAllowedError.class, () -> sut.authenticate(managerParams));
    verify(createUserRepository, never()).create(any(), anyString());
  }

  @Test
  void throwsWasherSelfRegistrationNotAllowedWhenPhoneNotRegisteredAndExpectingWasher() {
    var washerParams =
        new AuthenticateUser.Params(
            "11999992223", "Lavador Novo", "senha-segura", UserProfile.WASHER);
    when(loadUserByPhoneRepository.loadByPhone(washerParams.phone())).thenReturn(Optional.empty());

    assertThrows(WasherSelfRegistrationNotAllowedError.class, () -> sut.authenticate(washerParams));
    verify(createUserRepository, never()).create(any(), anyString());
  }
}
