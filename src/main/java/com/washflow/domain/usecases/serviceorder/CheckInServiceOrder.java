package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
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
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError;

  /** {@code washerId}: the washer the manager reserves the car for; null for the shared queue. */
  record Params(UUID serviceOrderId, UUID washerId) {

    public Params(UUID serviceOrderId) {
      this(serviceOrderId, null);
    }
  }
}
