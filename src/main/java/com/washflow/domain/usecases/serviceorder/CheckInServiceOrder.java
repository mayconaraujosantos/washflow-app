package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.errors.vehicle.VehicleAlreadyInYardError;
import java.util.UUID;

/**
 * Checks a vehicle into the yard, moving its {@code atendimento} from {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#SCHEDULED} to {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#WAITING_IN_YARD} - the point where
 * it enters the washers' queue in the PWA. Implemented by {@code DbCheckInServiceOrder} in the data
 * layer.
 */
public interface CheckInServiceOrder {

  ServiceOrder checkIn(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError, VehicleAlreadyInYardError;

  record Params(UUID serviceOrderId) {}
}
