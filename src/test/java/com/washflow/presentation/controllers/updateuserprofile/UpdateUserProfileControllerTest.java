package com.washflow.presentation.controllers.updateuserprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserProfile;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
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
class UpdateUserProfileControllerTest {

  @Mock private UpdateUserProfile updateUserProfile;
  @Mock private Validation validation;

  private UpdateUserProfileController sut;
  private UUID userId;

  @BeforeEach
  void setUp() {
    sut = new UpdateUserProfileController(updateUserProfile, validation);
    userId = UUID.randomUUID();
  }

  @Test
  void updatesUserProfileAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("managerId", UUID.randomUUID().toString());
    body.put("newProfile", "MANAGER");
    HttpRequest request = new HttpRequest(body, Map.of("id", userId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(null);
    User saved = new User(userId, "User", "11999999999", UserProfile.MANAGER, Instant.now(), true);
    when(updateUserProfile.update(any(UpdateUserProfile.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(updateUserProfile).update(any(UpdateUserProfile.Params.class));
  }

  @Test
  void returnsBadRequestWhenUserIdIsInvalid() {
    Map<String, Object> body = new HashMap<>();
    body.put("managerId", UUID.randomUUID().toString());
    body.put("newProfile", "MANAGER");
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    when(validation.validate(body)).thenReturn(null);

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenUserNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("managerId", UUID.randomUUID().toString());
    body.put("newProfile", "MANAGER");
    HttpRequest request = new HttpRequest(body, Map.of("id", userId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(updateUserProfile.update(any(UpdateUserProfile.Params.class)))
        .thenThrow(new UserNotFoundError(userId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }
}
