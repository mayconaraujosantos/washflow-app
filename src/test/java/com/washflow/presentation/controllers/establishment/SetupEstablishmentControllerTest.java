package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.establishment.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.establishment.InvalidSetupCodeError;
import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SetupEstablishmentControllerTest {

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

  @Test
  void configuresThePlatformOwner() throws Exception {
    SetupEstablishment setup = mock(SetupEstablishment.class);

    HttpResponse response =
        new SetupEstablishmentController(setup)
            .handle(
                body(
                    "setupCode",
                    "c",
                    "ownerName",
                    "Ana",
                    "ownerEmail",
                    "a@x.test",
                    "ownerPassword",
                    "secret-123"));

    assertEquals(200, response.statusCode());
    verify(setup).setup(new SetupEstablishment.Params("c", "Ana", "a@x.test", "secret-123"));
  }

  @Test
  void mapsWrongCodeAlreadyConfiguredAndWrongTypes() throws Exception {
    SetupEstablishment setup = mock(SetupEstablishment.class);
    when(setup.setup(any()))
        .thenThrow(new InvalidSetupCodeError())
        .thenThrow(new EstablishmentAlreadyConfiguredError());
    SetupEstablishmentController sut = new SetupEstablishmentController(setup);

    assertEquals(403, sut.handle(body("setupCode", "x")).statusCode());
    assertEquals(409, sut.handle(body("setupCode", "x")).statusCode());
    assertEquals(400, sut.handle(body("setupCode", 1)).statusCode());
  }
}
