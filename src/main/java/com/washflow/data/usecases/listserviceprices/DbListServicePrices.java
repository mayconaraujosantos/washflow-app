package com.washflow.data.usecases.listserviceprices;

import com.washflow.data.protocols.db.LoadAllServicePricesRepository;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.usecases.ListServicePrices;
import java.util.List;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbListServicePrices implements ListServicePrices {

  private final LoadAllServicePricesRepository loadAllServicePricesRepository;

  public DbListServicePrices(LoadAllServicePricesRepository loadAllServicePricesRepository) {
    this.loadAllServicePricesRepository = loadAllServicePricesRepository;
  }

  @Override
  public List<ServicePrice> list() {
    return loadAllServicePricesRepository.loadAll();
  }
}
