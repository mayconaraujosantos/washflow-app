package com.washflow.data.usecases.whatsapp;

import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.vehicle.Vehicle;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How the assistant writes days, hours, cars, prices and statuses (shop-local, pt-BR). */
final class AssistantTexts {

  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter HOUR =
      DateTimeFormatter.ofPattern("HH:mm").withZone(SchedulingRules.STORE_ZONE);
  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
  private static final String[] WEEKDAYS = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};

  private AssistantTexts() {}

  static String day(LocalDate day, LocalDate today) {
    String date = weekday(day.getDayOfWeek()) + " " + DAY_MONTH.format(day);
    if (day.equals(today)) {
      return "Hoje (" + date + ")";
    }
    if (day.equals(today.plusDays(1))) {
      return "Amanhã (" + date + ")";
    }
    return date;
  }

  static String hour(Instant instant) {
    return HOUR.format(instant);
  }

  static String when(Instant instant, LocalDate today) {
    LocalDate day = instant.atZone(SchedulingRules.STORE_ZONE).toLocalDate();
    String label = day(day, today);
    String lower = Character.toLowerCase(label.charAt(0)) + label.substring(1);
    return lower + " às " + hour(instant);
  }

  static String vehicle(Vehicle vehicle) {
    StringBuilder text = new StringBuilder();
    if (vehicle.model() != null && !vehicle.model().isBlank()) {
      text.append(vehicle.model().trim()).append(' ');
    }
    if (vehicle.color() != null && !vehicle.color().isBlank()) {
      text.append(vehicle.color().trim().toLowerCase(PT_BR)).append(' ');
    }
    return text.append('(').append(vehicle.plate()).append(')').toString();
  }

  static String vehicle(ServiceOrder.Details details) {
    String model = details.vehicleModel();
    return (model == null || model.isBlank() ? "Carro" : model.trim())
        + " ("
        + details.vehiclePlate()
        + ")";
  }

  static String money(BigDecimal amount) {
    return String.format(PT_BR, "R$ %,.2f", amount);
  }

  static String status(ServiceOrderStatus status) {
    return switch (status) {
      case SCHEDULED -> "agendado";
      case WAITING_IN_YARD -> "aguardando no pátio";
      case WASHING -> "em lavagem";
      case PAUSED -> "lavagem pausada";
      case READY -> "pronto para retirada";
      case DONE -> "finalizado";
      case CANCELLED -> "cancelado";
      case REFUNDED -> "estornado";
    };
  }

  static String firstName(String name) {
    return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0];
  }

  private static String weekday(DayOfWeek day) {
    return WEEKDAYS[day.getValue() - 1];
  }
}
