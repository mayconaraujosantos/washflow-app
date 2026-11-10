package com.washflow.presentation.controllers.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScheduleAvailabilityControllerTest {

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
  void returnsTheSlotsOfADay() {
    ScheduleAvailability availability = mock(ScheduleAvailability.class);
    ScheduleAvailability.Day day =
        new ScheduleAvailability.Day(LocalDate.parse("2026-11-11"), true, List.of());
    when(availability.forDate(LocalDate.parse("2026-11-11"))).thenReturn(day);

    HttpResponse response =
        new ScheduleAvailabilityController(availability).handle(query("date", "2026-11-11"));

    assertEquals(200, response.statusCode());
    assertEquals(day, response.body());
  }

  @Test
  void rejectsAMissingOrInvalidDate() {
    ScheduleAvailability availability = mock(ScheduleAvailability.class);
    ScheduleAvailabilityController sut = new ScheduleAvailabilityController(availability);

    assertEquals(400, sut.handle(query()).statusCode());
    assertEquals(400, sut.handle(query("date", "amanha")).statusCode());
    verifyNoInteractions(availability);
  }
}
