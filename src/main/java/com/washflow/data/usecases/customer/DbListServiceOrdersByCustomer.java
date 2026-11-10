package com.washflow.data.usecases.customer;

import com.washflow.data.protocols.db.serviceorder.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.customer.ListServiceOrdersByCustomer;
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
