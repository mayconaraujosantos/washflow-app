package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import java.util.UUID;

/**
 * The manager closes out the sale: the client has paid, moving the {@code atendimento} from {@link
 * com.washflow.domain.entities.ServiceOrderStatus#READY} to {@link
 * com.washflow.domain.entities.ServiceOrderStatus#DONE} and crediting the washer's exact commission
 * for that service. Implemented by {@code DbFinalizeServiceOrder} in the data layer.
 */
public interface FinalizeServiceOrder {

  ServiceOrder finalize(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError, ServicePriceNotFoundError;

  record Params(UUID serviceOrderId) {}
}
