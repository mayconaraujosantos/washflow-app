package com.washflow.domain.usecases.customer;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;

/**
 * Lists a customer's service orders (across all their vehicles) - backs the status-tracking panel
 * (RF-06) for a client who doesn't have a specific service order id at hand, e.g. a fresh login on
 * a new device. Implemented by {@code DbListServiceOrdersByCustomer} in the data layer.
 */
public interface ListServiceOrdersByCustomer {

  List<ServiceOrder> list(Params params);

  record Params(UUID customerId) {}
}
