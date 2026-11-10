package com.washflow.data.usecases.vehicle;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.vehicle.CreateVehicleRepository;
import com.washflow.data.protocols.db.vehicle.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.errors.vehicle.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.vehicle.CreateVehicle;
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
