package com.washflow.data.usecases.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DbScheduleAvailabilityTest {

  // Tuesday 2026-11-03, 10:30 in Manaus (UTC-4).
  private static final Clock NOW =
      Clock.fixed(Instant.parse("2026-11-03T14:30:00Z"), ZoneOffset.UTC);
  private static final Instant TEN_AM = Instant.parse("2026-11-03T14:00:00Z");
  private static final Instant ELEVEN_AM = Instant.parse("2026-11-03T15:00:00Z");

  private static DbScheduleAvailability sut(boolean openOnSunday) {
    // 2 bookings at 11h (full with capacity 2), none elsewhere.
    return new DbScheduleAvailability(
        () -> new SchedulingRules.Hours(8, 18, openOnSunday, 2),
        (start, end) -> start.equals(ELEVEN_AM) ? 2 : 0,
        NOW);
  }

  @Test
  void listsOpeningHoursMarkingPastAndFullSlots() {
    ScheduleAvailability.Day day = sut(false).forDate(LocalDate.of(2026, 11, 3));

    assertTrue(day.open());
    assertEquals(10, day.slots().size());
    assertEquals(8, day.slots().get(0).hour());
    assertFalse(slot(day, 10).available(), "10h already started");
    assertEquals(TEN_AM, slot(day, 10).startsAt());
    assertFalse(slot(day, 11).available(), "11h is full");
    assertEquals(2, slot(day, 11).booked());
    assertTrue(slot(day, 12).available());
  }

  @Test
  void closedSundaysHaveNoSlotsUnlessTheShopOpens() {
    LocalDate sunday = LocalDate.of(2026, 11, 8);

    assertFalse(sut(false).forDate(sunday).open());
    assertTrue(sut(false).forDate(sunday).slots().isEmpty());
    assertTrue(sut(true).forDate(sunday).open());
  }

  @Test
  void rejectsPastDatesAndTheFarFuture() {
    assertThrows(
        IllegalArgumentException.class, () -> sut(false).forDate(LocalDate.of(2026, 11, 2)));
    assertThrows(
        IllegalArgumentException.class, () -> sut(false).forDate(LocalDate.of(2027, 2, 1)));
  }

  private static ScheduleAvailability.Slot slot(ScheduleAvailability.Day day, int hour) {
    return day.slots().stream().filter(s -> s.hour() == hour).findFirst().orElseThrow();
  }
}
