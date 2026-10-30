package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import java.util.UUID;

/**
 * Checks a vehicle into the yard, moving its {@code atendimento} from {@link
 * com.washflow.domain.entities.ServiceOrderStatus#SCHEDULED} to {@link
 * com.washflow.domain.entities.ServiceOrderStatus#WAITING_IN_YARD} - the point where it enters the
 * washers' queue in the PWA. Implemented by {@code DbCheckInServiceOrder} in the data layer.
 */
public interface CheckInServiceOrder {

  ServiceOrder checkIn(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError;

  record Params(UUID serviceOrderId) {}
}
