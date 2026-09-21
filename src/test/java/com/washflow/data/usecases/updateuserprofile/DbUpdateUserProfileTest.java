package com.washflow.data.usecases.updateuserprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.UpdateUserProfileRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ForbiddenError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserProfile;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbCheckInServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbUpdateUserProfileTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private UpdateUserProfileRepository updateUserProfileRepository;

  private DbUpdateUserProfile sut;
  private User manager;
  private User target;
  private UpdateUserProfile.Params params;

  @BeforeEach
  void setUp() {
    sut = new DbUpdateUserProfile(loadUserByIdRepository, updateUserProfileRepository);

    manager =
        new User(
            UUID.randomUUID(), "Gerente Demo", "11999992222", UserProfile.MANAGER, Instant.now());
    target =
        new User(
            UUID.randomUUID(), "Lavador Demo", "11999991111", UserProfile.WASHER, Instant.now());
    params = new UpdateUserProfile.Params(manager.id(), target.id(), UserProfile.MANAGER);
  }

  @Test
  void updatesTargetProfileWhenRequesterIsManager() throws Exception {
    when(loadUserByIdRepository.loadById(manager.id())).thenReturn(Optional.of(manager));
    when(loadUserByIdRepository.loadById(target.id())).thenReturn(Optional.of(target));
    User promoted =
        new User(
            target.id(), target.name(), target.phone(), UserProfile.MANAGER, target.createdAt());
    when(updateUserProfileRepository.updateProfile(target.id(), UserProfile.MANAGER))
        .thenReturn(promoted);

    User result = sut.update(params);

    assertEquals(UserProfile.MANAGER, result.profile());
    verify(updateUserProfileRepository).updateProfile(target.id(), UserProfile.MANAGER);
  }

  @Test
  void throwsForbiddenWhenRequesterIsNotManager() {
    User nonManager =
        new User(
            UUID.randomUUID(), "Cliente Demo", "11999990000", UserProfile.CUSTOMER, Instant.now());
    var forbiddenParams =
        new UpdateUserProfile.Params(nonManager.id(), target.id(), UserProfile.MANAGER);
    when(loadUserByIdRepository.loadById(nonManager.id())).thenReturn(Optional.of(nonManager));

    assertThrows(ForbiddenError.class, () -> sut.update(forbiddenParams));
    verify(updateUserProfileRepository, never()).updateProfile(target.id(), UserProfile.MANAGER);
  }

  @Test
  void throwsUserNotFoundWhenRequesterDoesNotExist() {
    when(loadUserByIdRepository.loadById(manager.id())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.update(params));
    verify(updateUserProfileRepository, never()).updateProfile(target.id(), UserProfile.MANAGER);
  }

  @Test
  void throwsUserNotFoundWhenTargetDoesNotExist() {
    when(loadUserByIdRepository.loadById(manager.id())).thenReturn(Optional.of(manager));
    when(loadUserByIdRepository.loadById(target.id())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.update(params));
    verify(updateUserProfileRepository, never()).updateProfile(target.id(), UserProfile.MANAGER);
  }
}
