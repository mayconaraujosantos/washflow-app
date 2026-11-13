package com.washflow.presentation.controllers.schedule;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ListAgendaControllerTest {

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
