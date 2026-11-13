package com.washflow.data.protocols.db.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;

/** Port for loading the waiting queue of service orders. */
public interface LoadWaitingQueueRepository {

  /**
   * Cars waiting in the yard that {@code washerId} may start: the shared queue plus the ones
   * reserved for that washer (first). Null {@code washerId}: every waiting car.
   */
  List<ServiceOrder> loadWaitingQueue(UUID washerId);
}
