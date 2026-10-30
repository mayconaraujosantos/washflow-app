package com.washflow.domain.usecases;

import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.VehiclePlateAlreadyRegisteredError;
import java.util.UUID;

/**
 * Registers a vehicle for an existing customer - RF-03's "veículo (se já cadastrado)" step from
 * {@code docs/feature_espec_visao_do_cliente.md} starts here, the first time a plate is seen.
 * {@code plate} is globally unique (one physical vehicle can't belong to two records), so a repeat
 * registration attempt is rejected rather than silently returning the existing row - after the
 * first registration, the client should pick the vehicle from {@code ListVehiclesByCustomer}
 * instead. Implemented by {@code DbCreateVehicle} in the data layer.
 */
public interface CreateVehicle {

  Vehicle create(Params params) throws UserNotFoundError, VehiclePlateAlreadyRegisteredError;

  record Params(UUID customerId, String plate, String model, String color) {}
}
