package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;

/**
 * Returns the yard overview of service orders. Implemented by {@code DbGetYardOverview} in the data
 * layer.
 */
public interface GetYardOverview {

  List<ServiceOrder> get();
}
