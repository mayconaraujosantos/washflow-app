package com.washflow.data.usecases.listserviceordersbycustomer;

import com.washflow.data.protocols.db.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListServiceOrdersByCustomer;
import java.util.List;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbListServiceOrdersByCustomer implements ListServiceOrdersByCustomer {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final LoadServiceOrdersByCustomerIdRepository loadServiceOrdersByCustomerIdRepository;

  public DbListServiceOrdersByCustomer(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadServiceOrdersByCustomerIdRepository loadServiceOrdersByCustomerIdRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadServiceOrdersByCustomerIdRepository = loadServiceOrdersByCustomerIdRepository;
  }

  @Override
  public List<ServiceOrder> list(Params params) throws UserNotFoundError {
    loadUserByIdRepository
        .loadById(params.customerId())
        .orElseThrow(() -> new UserNotFoundError(params.customerId()));

    return loadServiceOrdersByCustomerIdRepository.loadByCustomerId(params.customerId());
  }
}
