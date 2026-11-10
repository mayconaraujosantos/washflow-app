package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SetupStatusControllerTest {

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
  void tellsWhetherThePlatformIsConfigured() {
    SetupEstablishment setup = mock(SetupEstablishment.class);
    SetupEstablishment.Status status = new SetupEstablishment.Status(false, true, null);
    when(setup.status()).thenReturn(status);

    HttpResponse response = new SetupStatusController(setup).handle(query());

    assertEquals(200, response.statusCode());
    assertEquals(status, response.body());
  }
}
