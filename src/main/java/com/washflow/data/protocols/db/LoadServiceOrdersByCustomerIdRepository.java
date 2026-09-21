package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;
import java.util.UUID;

public interface LoadServiceOrdersByCustomerIdRepository {

  List<ServiceOrder> loadByCustomerId(UUID customerId);
}
