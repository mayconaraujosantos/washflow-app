package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Vehicle;
import java.util.Optional;
import java.util.UUID;

public interface LoadVehicleByIdRepository {

  Optional<Vehicle> loadById(UUID id);
}
