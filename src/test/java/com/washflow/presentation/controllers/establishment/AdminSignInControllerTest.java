package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminSignInControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void signsInThePlatformAdministrator() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    User admin = mock(User.class);
    when(platform.signIn("admin@x.test", "secret-123")).thenReturn(admin);

    HttpResponse response =
        new AdminSignInController(platform)
            .handle(request(Map.of(), "email", "admin@x.test", "password", "secret-123"));

    assertEquals(200, response.statusCode());
    assertEquals(admin, response.body());
  }

  @Test
  void mapsWrongCredentialsAndWrongTypes() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.signIn(any(), any())).thenThrow(new InvalidCredentialsError());
    AdminSignInController sut = new AdminSignInController(platform);

    assertEquals(401, sut.handle(request(Map.of(), "email", "a", "password", "b")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "email", "a", "password", 123)).statusCode());
  }
}
