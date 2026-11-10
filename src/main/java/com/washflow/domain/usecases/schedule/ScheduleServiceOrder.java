package com.washflow.domain.usecases.schedule;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import java.time.Instant;
import java.util.UUID;

/**
 * Schedules a new {@code atendimento}, starting it at {@link
 * com.washflow.domain.entities.ServiceOrderStatus#SCHEDULED}. Enforces the MVP scheduling rules
 * from {@code docs/feature.spec.md}: business hours, minimum lead time, and yard capacity per hour.
 * Implemented by {@code DbScheduleServiceOrder} in the data layer.
 */
public interface ScheduleServiceOrder {

  ServiceOrder schedule(Params params)
      throws VehicleNotFoundError,
          ServicePriceNotFoundError,
          OutsideBusinessHoursError,
          InsufficientLeadTimeError,
          SlotFullyBookedError;

  record Params(UUID vehicleId, int servicePriceId, Instant scheduledAt) {}
}
