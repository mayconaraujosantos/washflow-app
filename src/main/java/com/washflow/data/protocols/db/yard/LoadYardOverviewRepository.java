package com.washflow.data.protocols.db.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;

/** Port for loading the yard overview of service orders. */
public interface LoadYardOverviewRepository {

  List<ServiceOrder> loadYardOverview();
}
