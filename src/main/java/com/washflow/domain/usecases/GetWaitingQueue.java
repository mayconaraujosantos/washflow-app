package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;

/**
 * Returns the waiting queue of service orders. Implemented by {@code DbGetWaitingQueue} in the data
 * layer.
 */
public interface GetWaitingQueue {

  List<ServiceOrder> get();
}
