package com.washflow.domain.usecases.schedule;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Schedules a new {@code atendimento}, starting it at {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#SCHEDULED}. Enforces the MVP
 * scheduling rules from {@code docs/feature.spec.md}: business hours, minimum lead time, and yard
 * capacity per hour. Implemented by {@code DbScheduleServiceOrder} in the data layer.
 */
public interface ScheduleServiceOrder {

  ServiceOrder schedule(Params params);

  record Params(
      UUID vehicleId, int servicePriceId, List<Integer> servicePriceIds, Instant scheduledAt) {
    public Params {
      servicePriceIds = servicePriceIds == null ? null : List.copyOf(servicePriceIds);
    }

    public Params(UUID vehicleId, int servicePriceId, Instant scheduledAt) {
      this(vehicleId, servicePriceId, List.of(servicePriceId), scheduledAt);
    }
  }
}
