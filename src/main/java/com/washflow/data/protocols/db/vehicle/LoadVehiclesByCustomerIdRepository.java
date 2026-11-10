package com.washflow.data.protocols.db.vehicle;

import com.washflow.domain.entities.vehicle.Vehicle;
import java.util.List;
import java.util.UUID;

public interface LoadVehiclesByCustomerIdRepository {

  List<Vehicle> loadByCustomerId(UUID customerId);
}
