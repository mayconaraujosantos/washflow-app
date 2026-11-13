package com.washflow.domain.usecases.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;

/**
 * Returns the waiting queue of service orders. Implemented by {@code DbGetWaitingQueue} in the data
 * layer.
 */
public interface GetWaitingQueue {

  /** The cars {@code washerId} may start: reserved for them first, then the shared queue. */
  List<ServiceOrder> get(UUID washerId);
}
