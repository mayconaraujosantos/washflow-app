package com.washflow.data.protocols.db.vehicle;

import com.washflow.domain.entities.vehicle.Vehicle;

public interface CreateVehicleRepository {

  Vehicle create(Vehicle vehicle);
}
