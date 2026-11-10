package com.washflow.presentation.controllers.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.shared.CheckDatabaseHealthRepository;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

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
  void reportsTheDatabaseState() {
    CheckDatabaseHealthRepository database = mock(CheckDatabaseHealthRepository.class);
    when(database.isUp()).thenReturn(true).thenReturn(false);
    HealthController sut = new HealthController(database);

    HttpResponse up = sut.handle(query());
    HttpResponse down = sut.handle(query());

    assertEquals(200, up.statusCode());
    assertEquals(Map.of("status", "ok", "service", "washflow-api", "database", "up"), up.body());
    assertEquals("down", ((Map<?, ?>) down.body()).get("database"));
  }
}
