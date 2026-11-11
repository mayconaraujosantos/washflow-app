package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import java.util.UUID;

/**
 * The manager closes out the sale: the client has paid, moving the {@code atendimento} from {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#READY} to {@link
 * com.washflow.domain.entities.serviceorder.ServiceOrderStatus#DONE} and crediting the washer's
 * summed commissions for the selected services. Implemented by {@code DbFinalizeServiceOrder} in
 * the data layer.
 */
public interface FinalizeServiceOrder {

  ServiceOrder finalizeOrder(Params params)
      throws ServiceOrderNotFoundError, InvalidServiceOrderStatusError, ServicePriceNotFoundError;

  record Params(UUID serviceOrderId, PaymentMethod paymentMethod) {}
}
