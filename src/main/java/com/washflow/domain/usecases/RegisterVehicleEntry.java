package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.InvalidPlateError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.errors.VehicleAlreadyInYardError;
import java.time.Instant;

/**
 * The manager opens a service order at the counter, identified by the vehicle's plate. The vehicle
 * (and its walk-in contact) is created on the fly when the plate is new. Without {@code
 * scheduledAt} the car is already in the shop and goes straight to {@link
 * com.washflow.domain.entities.ServiceOrderStatus#WAITING_IN_YARD}; with it, the order is {@link
 * com.washflow.domain.entities.ServiceOrderStatus#SCHEDULED} under the same business-hours and
 * capacity rules customers follow.
 */
public interface RegisterVehicleEntry {

  ServiceOrder register(Params params)
      throws InvalidPlateError,
          ServicePriceNotFoundError,
          OutsideBusinessHoursError,
          InsufficientLeadTimeError,
          SlotFullyBookedError,
          VehicleAlreadyInYardError;

  record Params(
      String plate,
      String vehicleModel,
      String vehicleColor,
      String customerName,
      String customerPhone,
      int servicePriceId,
      Instant scheduledAt,
      Instant expectedDeliveryAt,
      String notes) {}
}
