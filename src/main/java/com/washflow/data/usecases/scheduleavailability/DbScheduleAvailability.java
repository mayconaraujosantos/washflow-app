package com.washflow.data.usecases.scheduleavailability;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DbScheduleAvailability implements ScheduleAvailability {

  private static final int MAX_DAYS_AHEAD = 60;

  private final Supplier<SchedulingRules.Hours> hours;
  private final CountServiceOrdersScheduledInRangeRepository countRepository;
  private final Clock clock;

  public DbScheduleAvailability(
      Supplier<SchedulingRules.Hours> hours,
      CountServiceOrdersScheduledInRangeRepository countRepository,
      Clock clock) {
    this.hours = hours;
    this.countRepository = countRepository;
    this.clock = clock;
  }

  @Override
  public Day forDate(LocalDate date) {
    Instant now = Instant.now(clock);
    LocalDate today = now.atZone(SchedulingRules.STORE_ZONE).toLocalDate();
    if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
      throw new IllegalArgumentException(
          "Escolha uma data entre hoje e os próximos " + MAX_DAYS_AHEAD + " dias");
    }
    SchedulingRules.Hours current = hours.get();
    if (date.getDayOfWeek() == DayOfWeek.SUNDAY && !current.openOnSunday()) {
      return new Day(date, false, List.of());
    }
    List<Slot> slots = new ArrayList<>();
    for (int hour = current.openingHour(); hour < current.closingHour(); hour++) {
      Instant start = date.atTime(hour, 0).atZone(SchedulingRules.STORE_ZONE).toInstant();
      int booked = countRepository.countScheduledBetween(start, start.plus(1, ChronoUnit.HOURS));
      int capacity = current.maxVehiclesPerHour();
      slots.add(new Slot(start, hour, booked, capacity, booked < capacity && start.isAfter(now)));
    }
    return new Day(date, true, slots);
  }
}
