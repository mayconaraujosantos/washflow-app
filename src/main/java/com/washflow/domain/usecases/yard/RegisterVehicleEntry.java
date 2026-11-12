package com.washflow.domain.usecases.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.time.Instant;

/**
 * The manager opens a service order at the counter, identified by the vehicle's plate. The vehicle
 * (and its walk-in contact) is created on the fly when the plate is new. Without {@code
 * scheduledAt} the car is already in the shop and goes straight to {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#WAITING_IN_YARD}; with it, the order
 * is {@link com.washflow.domain.entities.serviceorder.ServiceOrderStatus#SCHEDULED} under the same
 * business-hours and capacity rules customers follow.
 */
public interface RegisterVehicleEntry {

  ServiceOrder register(Params params);

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
