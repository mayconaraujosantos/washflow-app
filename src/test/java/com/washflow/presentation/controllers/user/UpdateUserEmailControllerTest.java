package com.washflow.presentation.controllers.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.UpdateUserEmail;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateUserEmailControllerTest {

  private static HttpRequest body(Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, Map.of(), Map.of());
  }

  private static HttpRequest query(String... pairs) {
    Map<String, String> query = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      query.put(pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(Map.of(), Map.of(), query);
  }

  private static HttpRequest forUser(UUID id, Object... pairs) {
    return new HttpRequest(body(pairs).body(), Map.of("id", id.toString()), Map.of());
  }

  @Test
  void setsOrClearsTheSignInEmail() throws Exception {
    UpdateUserEmail update = mock(UpdateUserEmail.class);
    UpdateUserEmailController sut = new UpdateUserEmailController(update);
    UUID id = UUID.randomUUID();

    assertEquals(200, sut.handle(forUser(id, "email", "ana@x.test")).statusCode());
    assertEquals(200, sut.handle(forUser(id)).statusCode());
    verify(update).update(id, "ana@x.test");
    verify(update).update(id, null);
  }

  @Test
  void mapsWrongTypeUnknownUserAndTakenEmail() throws Exception {
    UpdateUserEmail update = mock(UpdateUserEmail.class);
    when(update.update(any(), any()))
        .thenThrow(new UserNotFoundError(UUID.randomUUID()))
        .thenThrow(new EmailAlreadyRegisteredError());
    UpdateUserEmailController sut = new UpdateUserEmailController(update);
    UUID id = UUID.randomUUID();

    assertEquals(400, sut.handle(forUser(id, "email", 123)).statusCode());
    assertEquals(404, sut.handle(forUser(id, "email", "a@x.test")).statusCode());
    assertEquals(409, sut.handle(forUser(id, "email", "a@x.test")).statusCode());
  }
}
