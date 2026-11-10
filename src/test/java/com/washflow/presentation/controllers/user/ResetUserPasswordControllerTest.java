package com.washflow.presentation.controllers.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.PasswordResetNotAllowedError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.ResetUserPassword;
import com.washflow.presentation.protocols.HttpRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResetUserPasswordControllerTest {

  private final UUID id = UUID.randomUUID();

  private HttpRequest request(Map<String, Object> body) {
    return new HttpRequest(body, Map.of("id", id.toString()), Map.of());
  }

  @Test
  void resetsAndReturnsTheAccount() throws Exception {
    ResetUserPassword reset = mock(ResetUserPassword.class);
    when(reset.reset(id, "nova-senha-123"))
        .thenReturn(new User(id, "Ana", "92991234567", UserProfile.CUSTOMER, Instant.now(), true));

    var response =
        new ResetUserPasswordController(reset)
            .handle(request(Map.of("password", "nova-senha-123")));

    assertEquals(200, response.statusCode());
    verify(reset).reset(id, "nova-senha-123");
  }

  @Test
  void mapsInvalidInputUnknownAccountAndManagerAccount() throws Exception {
    ResetUserPassword reset = mock(ResetUserPassword.class);
    when(reset.reset(any(), any()))
        .thenThrow(new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres"))
        .thenThrow(new UserNotFoundError(id))
        .thenThrow(new PasswordResetNotAllowedError());
    ResetUserPasswordController sut = new ResetUserPasswordController(reset);

    assertEquals(400, sut.handle(request(Map.of("password", "x"))).statusCode());
    assertEquals(404, sut.handle(request(Map.of("password", "nova-senha-123"))).statusCode());
    assertEquals(403, sut.handle(request(Map.of("password", "nova-senha-123"))).statusCode());
    assertEquals(400, sut.handle(request(Map.of("password", 12345678))).statusCode());
    assertEquals(
        400,
        sut.handle(new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of())).statusCode());
  }
}
