package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import java.util.UUID;

/**
 * The washer finishes the physical wash, moving the {@code atendimento} from {@link
 * com.washflow.domain.entities.ServiceOrderStatus#WASHING} to {@link
 * com.washflow.domain.entities.ServiceOrderStatus#READY} - the point where the client is notified
 * the vehicle is ready for pickup. Implemented by {@code DbCompleteServiceOrder} in the data layer.
 */
public interface CompleteServiceOrder {

  ServiceOrder complete(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError;

  record Params(UUID serviceOrderId) {}
}
