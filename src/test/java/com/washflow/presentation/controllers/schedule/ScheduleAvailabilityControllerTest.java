package com.washflow.presentation.controllers.schedule;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleAvailabilityControllerTest {

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
