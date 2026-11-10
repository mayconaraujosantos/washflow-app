package com.washflow.domain.usecases.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;

/**
 * Returns the waiting queue of service orders. Implemented by {@code DbGetWaitingQueue} in the data
 * layer.
 */
public interface GetWaitingQueue {

  List<ServiceOrder> get();
}
