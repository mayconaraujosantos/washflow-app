package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Vehicle;
import java.util.List;
import java.util.UUID;

public interface LoadVehiclesByCustomerIdRepository {

  List<Vehicle> loadByCustomerId(UUID customerId);
}
