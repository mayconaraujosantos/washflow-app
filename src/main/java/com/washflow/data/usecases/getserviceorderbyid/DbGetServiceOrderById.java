package com.washflow.data.usecases.getserviceorderbyid;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.GetServiceOrderById;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbGetServiceOrderById implements GetServiceOrderById {

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;

  public DbGetServiceOrderById(LoadServiceOrderByIdRepository loadServiceOrderByIdRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
  }

  @Override
  public ServiceOrder get(Params params) throws ServiceOrderNotFoundError {
    return loadServiceOrderByIdRepository
        .loadById(params.serviceOrderId())
        .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));
  }
}
