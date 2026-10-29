package com.washflow.data.usecases.updateuseractive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.UpdateUserActiveRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserHasActiveWashesError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserActive;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbUpdateUserActiveTest {

  @Mock private UpdateUserActiveRepository updateRepository;

  private DbUpdateUserActive sut;
  private UUID userId;
  private User testUser;

  @BeforeEach
  void setUp() {
    sut = new DbUpdateUserActive(updateRepository);
    userId = UUID.randomUUID();
    testUser =
        new User(userId, "Test User", "11999999999", UserProfile.WASHER, Instant.now(), true);
  }

  @Test
  void activatesUserSuccessfully() throws Exception {
    when(updateRepository.updateActive(eq(userId), eq(true))).thenReturn(testUser);

    UpdateUserActive.Params params = new UpdateUserActive.Params(userId, true);

    User result = sut.update(params);

    assertEquals(testUser, result);
    verify(updateRepository).updateActive(userId, true);
  }

  @Test
  void deactivatesUserSuccessfully() throws Exception {
    User deactivatedUser =
        new User(userId, "Test User", "11999999999", UserProfile.WASHER, Instant.now(), false);
    when(updateRepository.updateActive(eq(userId), eq(false))).thenReturn(deactivatedUser);

    UpdateUserActive.Params params = new UpdateUserActive.Params(userId, false);

    User result = sut.update(params);

    assertEquals(false, result.active());
    verify(updateRepository).updateActive(userId, false);
  }

  @Test
  void throwsUserHasActiveWashesErrorWhenWasherHasActiveOrders() {
    when(updateRepository.updateActive(eq(userId), eq(false)))
        .thenThrow(new UserHasActiveWashesError(userId));

    UpdateUserActive.Params params = new UpdateUserActive.Params(userId, false);

    assertThrows(UserHasActiveWashesError.class, () -> sut.update(params));
  }

  @Test
  void throwsUserNotFoundErrorWhenUserDoesNotExist() {
    when(updateRepository.updateActive(eq(userId), eq(true)))
        .thenThrow(new RuntimeException("User not found"));

    UpdateUserActive.Params params = new UpdateUserActive.Params(userId, true);

    assertThrows(UserNotFoundError.class, () -> sut.update(params));
  }
}
