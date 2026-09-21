package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import java.util.UUID;

/**
 * Assigns a washer to the vehicle and moves its {@code atendimento} from {@link
 * com.washflow.domain.entities.ServiceOrderStatus#WAITING_IN_YARD} to {@link
 * com.washflow.domain.entities.ServiceOrderStatus#WASHING} - the washer has pulled the car in and
 * started working on it. Implemented by {@code DbStartServiceOrder} in the data layer.
 */
public interface StartServiceOrder {

  ServiceOrder start(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError;

  record Params(UUID serviceOrderId, UUID washerId) {}
}
