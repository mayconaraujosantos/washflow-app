package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;

/** Port for loading the waiting queue of service orders. */
public interface LoadWaitingQueueRepository {

  List<ServiceOrder> loadWaitingQueue();
}
