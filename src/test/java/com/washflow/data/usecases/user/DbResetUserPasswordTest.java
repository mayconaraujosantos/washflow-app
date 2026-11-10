package com.washflow.data.usecases.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.PasswordResetNotAllowedError;
import com.washflow.domain.errors.user.UserNotFoundError;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbResetUserPasswordTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  @Mock private PasswordHasher passwordHasher;

  private DbResetUserPassword sut;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut =
        new DbResetUserPassword(
            loadUserByIdRepository, saveUserPasswordHashRepository, passwordHasher);
  }

  private User user(UserProfile profile) {
    return new User(userId, "Ana", "92991234567", profile, Instant.now(), true);
  }

  @ParameterizedTest
  @EnumSource(
      value = UserProfile.class,
      names = {"CUSTOMER", "WASHER"})
  void storesOnlyTheHashOfTheNewPassword(UserProfile profile) throws Exception {
    when(loadUserByIdRepository.loadById(userId)).thenReturn(Optional.of(user(profile)));
    when(passwordHasher.hash("nova-senha-123")).thenReturn("bcrypt-hash");

    User result = sut.reset(userId, "nova-senha-123");

    assertEquals(userId, result.id());
    verify(saveUserPasswordHashRepository).savePasswordHash(userId, "bcrypt-hash");
  }

  @Test
  void refusesShortPasswordsBeforeTouchingTheAccount() {
    assertThrows(IllegalArgumentException.class, () -> sut.reset(userId, "curta"));
    assertThrows(IllegalArgumentException.class, () -> sut.reset(userId, null));
    verify(loadUserByIdRepository, never()).loadById(any());
  }

  @Test
  void unknownOrOtherLavaJatoAccountIsNotFound() {
    when(loadUserByIdRepository.loadById(userId)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.reset(userId, "nova-senha-123"));
    verify(saveUserPasswordHashRepository, never()).savePasswordHash(any(), any());
  }

  @Test
  void managersPasswordIsLeftToThePlatformAdministrator() {
    when(loadUserByIdRepository.loadById(userId))
        .thenReturn(Optional.of(user(UserProfile.MANAGER)));

    assertThrows(PasswordResetNotAllowedError.class, () -> sut.reset(userId, "nova-senha-123"));
    verify(saveUserPasswordHashRepository, never()).savePasswordHash(any(), any());
  }
}
