package com.washflow.presentation.controllers.system;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.shared.CheckDatabaseHealthRepository;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

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
