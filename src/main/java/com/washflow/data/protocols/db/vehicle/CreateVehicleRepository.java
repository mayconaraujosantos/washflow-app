package com.washflow.data.protocols.db.vehicle;

import com.washflow.domain.entities.Vehicle;

public interface CreateVehicleRepository {

  Vehicle create(Vehicle vehicle);
}
