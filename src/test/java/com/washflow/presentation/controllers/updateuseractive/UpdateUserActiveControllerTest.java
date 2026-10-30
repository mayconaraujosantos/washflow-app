package com.washflow.presentation.controllers.updateuseractive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserHasActiveWashesError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserActive;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUserActiveControllerTest {

  @Mock private UpdateUserActive updateUserActive;

  private UpdateUserActiveController sut;
  private UUID userId;

  @BeforeEach
  void setUp() {
    sut = new UpdateUserActiveController(updateUserActive);
    userId = UUID.randomUUID();
  }

  @Test
  void updatesUserActiveAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("active", true);
    HttpRequest request = new HttpRequest(body, Map.of("id", userId.toString()), Map.of());

    User saved = new User(userId, "User", "11999999999", UserProfile.WASHER, Instant.now(), true);
    when(updateUserActive.update(any(UpdateUserActive.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(updateUserActive).update(any(UpdateUserActive.Params.class));
  }

  @Test
  void returnsBadRequestWhenUserIdIsInvalid() {
    Map<String, Object> body = new HashMap<>();
    body.put("active", true);
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenUserNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("active", true);
    HttpRequest request = new HttpRequest(body, Map.of("id", userId.toString()), Map.of());

    when(updateUserActive.update(any(UpdateUserActive.Params.class)))
        .thenThrow(new UserNotFoundError(userId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenUserHasActiveWashes() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("active", false);
    HttpRequest request = new HttpRequest(body, Map.of("id", userId.toString()), Map.of());

    when(updateUserActive.update(any(UpdateUserActive.Params.class)))
        .thenThrow(new UserHasActiveWashesError(userId));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
