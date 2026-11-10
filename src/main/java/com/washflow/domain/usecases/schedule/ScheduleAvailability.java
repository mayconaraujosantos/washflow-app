package com.washflow.domain.usecases.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Bookable hours of one shop-local day: the same opening hours and per-hour capacity the scheduling
 * rules enforce, so the app only offers times that will be accepted.
 */
public interface ScheduleAvailability {

  Day forDate(LocalDate date);

  record Day(LocalDate date, boolean open, List<Slot> slots) {}

  /** {@code startsAt} is the slot's first instant; {@code available} = not past and not full. */
  record Slot(Instant startsAt, int hour, int booked, int capacity, boolean available) {}
}
