package com.washflow.data.protocols.ai;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads what a customer wrote in their own words ("quero lavar o Onix amanhã à tarde") into a
 * structured request. It only interprets: the assistant still checks every field against the
 * catalog, the customer's cars and the free hours, and the customer confirms before anything is
 * booked. Empty when the text can't be understood or the provider is unavailable.
 */
public interface MessageInterpreter {

  Optional<Interpretation> interpret(String text, Context context);

  /** What the customer can pick from, so the interpretation points at real ids. */
  record Context(LocalDate today, List<ServiceOption> services, List<VehicleOption> vehicles) {}

  record ServiceOption(int id, String name) {}

  record VehicleOption(UUID id, String description) {}

  /**
   * Every field but {@code intent} may be null: only what the customer actually said. {@code
   * period} is "MORNING" or "AFTERNOON".
   */
  record Interpretation(
      Intent intent,
      UUID vehicleId,
      String plate,
      List<Integer> serviceIds,
      LocalDate date,
      LocalTime time,
      String period) {

    public Interpretation {
      serviceIds = serviceIds == null ? List.of() : List.copyOf(serviceIds);
    }
  }

  enum Intent {
    SCHEDULE,
    MY_ORDERS,
    CANCEL,
    HUMAN,
    OTHER
  }
}
