package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Vehicle;

public interface CreateVehicleRepository {

  Vehicle create(Vehicle vehicle);
}
