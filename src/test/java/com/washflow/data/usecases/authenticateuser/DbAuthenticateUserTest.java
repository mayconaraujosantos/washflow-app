package com.washflow.data.usecases.authenticateuser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.usecases.AuthenticateUser;
import java.time.Instant;
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

  private DbAuthenticateUser sut;
  private AuthenticateUser.Params params;

  @BeforeEach
  void setUp() {
    sut = new DbAuthenticateUser(loadUserByPhoneRepository, createUserRepository);
    params = new AuthenticateUser.Params("11999990000", "Cliente Demo", UserProfile.CUSTOMER);
  }

  @Test
  void registersNewUserWithExpectedProfileWhenPhoneNotYetRegistered() throws Exception {
    when(loadUserByPhoneRepository.loadByPhone(params.phone())).thenReturn(Optional.empty());
    when(createUserRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

    User user = sut.authenticate(params);

    assertEquals(params.phone(), user.phone());
    assertEquals(params.name(), user.name());
    assertEquals(UserProfile.CUSTOMER, user.profile());
    verify(createUserRepository).create(any());
  }

  @Test
  void returnsExistingUserWhenProfileMatches() throws Exception {
    User existingUser =
        new User(
            UUID.randomUUID(), "Cliente Demo", params.phone(), UserProfile.CUSTOMER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.of(existingUser));

    User user = sut.authenticate(params);

    assertEquals(existingUser, user);
    verify(createUserRepository, never()).create(any());
  }

  @Test
  void throwsUserProfileMismatchWhenPhoneIsRegisteredUnderAnotherProfile() {
    User existingWasher =
        new User(
            UUID.randomUUID(), "Lavador Demo", params.phone(), UserProfile.WASHER, Instant.now());
    when(loadUserByPhoneRepository.loadByPhone(params.phone()))
        .thenReturn(Optional.of(existingWasher));

    assertThrows(UserProfileMismatchError.class, () -> sut.authenticate(params));
    verify(createUserRepository, never()).create(any());
  }

  @Test
  void throwsManagerSelfRegistrationNotAllowedWhenPhoneNotRegisteredAndExpectingManager() {
    var managerParams =
        new AuthenticateUser.Params("11999992222", "Gerente Novo", UserProfile.MANAGER);
    when(loadUserByPhoneRepository.loadByPhone(managerParams.phone())).thenReturn(Optional.empty());

    assertThrows(
        ManagerSelfRegistrationNotAllowedError.class, () -> sut.authenticate(managerParams));
    verify(createUserRepository, never()).create(any());
  }
}
