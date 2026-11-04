package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import java.util.Optional;
import java.util.UUID;

public interface UpdateServiceOrderStatusRepository {

  /**
   * Moves the order from {@code expected} to {@code status} in one conditional write. Empty when
   * the order is no longer in {@code expected} - another request changed it first (two washers
   * starting the same car, a double click) - so only one of the racing requests wins.
   */
  Optional<ServiceOrder> updateStatus(
      UUID id, ServiceOrderStatus expected, ServiceOrderStatus status, UUID washerId);
}
