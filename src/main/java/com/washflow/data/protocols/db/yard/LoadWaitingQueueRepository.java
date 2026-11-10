package com.washflow.data.protocols.db.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;

/** Port for loading the waiting queue of service orders. */
public interface LoadWaitingQueueRepository {

  List<ServiceOrder> loadWaitingQueue();
}
