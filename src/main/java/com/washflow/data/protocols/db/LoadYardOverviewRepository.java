package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;

/** Port for loading the yard overview of service orders. */
public interface LoadYardOverviewRepository {

  List<ServiceOrder> loadYardOverview();
}
