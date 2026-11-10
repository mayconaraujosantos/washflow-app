package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.establishment.EstablishmentNotFoundError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResetManagerPasswordControllerTest {

  private final UUID id = UUID.randomUUID();

  private HttpRequest request(Map<String, Object> body) {
    return new HttpRequest(body, Map.of("id", id.toString()), Map.of());
  }

  @Test
  void resetsTheManagerOfTheLavaJato() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);

    var response =
        new ResetManagerPasswordController(platform)
            .handle(request(Map.of("password", "nova-senha-123")));

    assertEquals(200, response.statusCode());
    verify(platform).resetManagerPassword(id, "nova-senha-123");
  }

  @Test
  void mapsShortPasswordAndUnknownLavaJato() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.resetManagerPassword(any(), any()))
        .thenThrow(new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres"))
        .thenThrow(new EstablishmentNotFoundError());
    ResetManagerPasswordController sut = new ResetManagerPasswordController(platform);

    assertEquals(400, sut.handle(request(Map.of("password", "x"))).statusCode());
    assertEquals(404, sut.handle(request(Map.of("password", "nova-senha-123"))).statusCode());
  }
}
