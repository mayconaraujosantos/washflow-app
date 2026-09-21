package com.washflow.data.usecases.listvehiclesbycustomer;

import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadVehiclesByCustomerIdRepository;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListVehiclesByCustomer;
import java.util.List;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbListVehiclesByCustomer implements ListVehiclesByCustomer {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final LoadVehiclesByCustomerIdRepository loadVehiclesByCustomerIdRepository;

  public DbListVehiclesByCustomer(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadVehiclesByCustomerIdRepository loadVehiclesByCustomerIdRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadVehiclesByCustomerIdRepository = loadVehiclesByCustomerIdRepository;
  }

  @Override
  public List<Vehicle> list(Params params) throws UserNotFoundError {
    loadUserByIdRepository
        .loadById(params.customerId())
        .orElseThrow(() -> new UserNotFoundError(params.customerId()));

    return loadVehiclesByCustomerIdRepository.loadByCustomerId(params.customerId());
  }
}
