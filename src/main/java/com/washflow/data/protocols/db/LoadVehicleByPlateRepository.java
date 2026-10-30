package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Vehicle;
import java.util.Optional;

public interface LoadVehicleByPlateRepository {

  Optional<Vehicle> loadByPlate(String plate);
}
