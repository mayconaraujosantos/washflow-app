package com.washflow.data.usecases.createvehicle;

import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateVehicle;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbCreateVehicle implements CreateVehicle {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final LoadVehicleByPlateRepository loadVehicleByPlateRepository;
  private final CreateVehicleRepository createVehicleRepository;

  public DbCreateVehicle(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadVehicleByPlateRepository loadVehicleByPlateRepository,
      CreateVehicleRepository createVehicleRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadVehicleByPlateRepository = loadVehicleByPlateRepository;
    this.createVehicleRepository = createVehicleRepository;
  }

  @Override
  public Vehicle create(Params params)
      throws UserNotFoundError, VehiclePlateAlreadyRegisteredError {
    loadUserByIdRepository
        .loadById(params.customerId())
        .orElseThrow(() -> new UserNotFoundError(params.customerId()));

    if (loadVehicleByPlateRepository.loadByPlate(params.plate()).isPresent()) {
      throw new VehiclePlateAlreadyRegisteredError(params.plate());
    }

    return createVehicleRepository.create(
        new Vehicle(
            UUID.randomUUID(),
            params.customerId(),
            params.plate(),
            params.model(),
            params.color()));
  }
}
