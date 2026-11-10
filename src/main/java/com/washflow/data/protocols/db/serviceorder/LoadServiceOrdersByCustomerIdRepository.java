package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;

public interface LoadServiceOrdersByCustomerIdRepository {

  List<ServiceOrder> loadByCustomerId(UUID customerId);
}
