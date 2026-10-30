package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;

public interface CreateServiceOrderRepository {

  ServiceOrder create(ServiceOrder serviceOrder);
}
