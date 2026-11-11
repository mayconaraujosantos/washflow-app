package com.washflow.domain.usecases.vehicle;

import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.vehicle.InvalidPlateError;
import com.washflow.domain.errors.vehicle.VehiclePlateAlreadyRegisteredError;
import java.util.UUID;

/**
 * Registers a vehicle for an existing customer - RF-03's "veículo (se já cadastrado)" step from
 * {@code docs/feature_espec_visao_do_cliente.md} starts here, the first time a plate is seen. The
 * plate is unique within the lava-jato, so a repeat registration is rejected - except for a car the
 * manager entered at the counter without an account and with this customer's phone as the contact:
 * that one is handed over to the customer (see {@link ListUnclaimedVehicles}). Implemented by
 * {@code DbCreateVehicle} in the data layer.
 */
public interface CreateVehicle {

  Vehicle create(Params params) throws InvalidPlateError, VehiclePlateAlreadyRegisteredError;

  record Params(UUID customerId, String plate, String model, String color) {}
}
