package com.washflow.data.protocols.db.shared;

import com.washflow.domain.entities.ServicePrice;
import java.util.List;

public interface LoadAllServicePricesRepository {

  List<ServicePrice> loadAll();
}
