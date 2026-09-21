package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import java.util.UUID;

public interface UpdateServiceOrderStatusRepository {

  ServiceOrder updateStatus(UUID id, ServiceOrderStatus status, UUID washerId);
}
