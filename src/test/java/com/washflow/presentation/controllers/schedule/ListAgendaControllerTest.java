package com.washflow.presentation.controllers.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListAgendaControllerTest {

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
  void listsTheAgendaOfARange() {
    ListAgenda agenda = mock(ListAgenda.class);

    HttpResponse response =
        new ListAgendaController(agenda).handle(query("from", "2026-11-01", "to", "2026-11-30"));

    assertEquals(200, response.statusCode());
    verify(agenda)
        .list(new ListAgenda.Params(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")));
  }

  @Test
  void rejectsMissingOrInvalidDates() {
    ListAgenda agenda = mock(ListAgenda.class);
    ListAgendaController sut = new ListAgendaController(agenda);

    assertEquals(400, sut.handle(query("to", "2026-11-30")).statusCode());
    assertEquals(400, sut.handle(query("from", "x", "to", "2026-11-30")).statusCode());
    verifyNoInteractions(agenda);
  }
}
