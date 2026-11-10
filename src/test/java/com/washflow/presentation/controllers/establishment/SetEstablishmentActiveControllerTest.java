package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.establishment.EstablishmentNotFoundError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SetEstablishmentActiveControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void suspendsOrReactivatesTheLavaJatoInThePath() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    UUID id = UUID.randomUUID();

    HttpResponse response =
        new SetEstablishmentActiveController(platform)
            .handle(request(Map.of("id", id.toString()), "active", false));

    assertEquals(200, response.statusCode());
    verify(platform).setActive(id, false);
  }

  @Test
  void requiresABooleanAndAKnownLavaJato() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.setActive(any(), org.mockito.ArgumentMatchers.anyBoolean()))
        .thenThrow(new EstablishmentNotFoundError());
    SetEstablishmentActiveController sut = new SetEstablishmentActiveController(platform);
    Map<String, String> path = Map.of("id", UUID.randomUUID().toString());

    assertEquals(400, sut.handle(request(path)).statusCode());
    assertEquals(400, sut.handle(request(path, "active", "nao")).statusCode());
    assertEquals(400, sut.handle(request(Map.of("id", "x"), "active", true)).statusCode());
    assertEquals(404, sut.handle(request(path, "active", true)).statusCode());
  }
}
