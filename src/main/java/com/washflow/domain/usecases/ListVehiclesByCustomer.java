package com.washflow.domain.usecases;

import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import java.util.List;
import java.util.UUID;

/**
 * Lists a customer's registered vehicles - backs the PWA's "veículo (se já cadastrado)" picker in
 * the scheduling flow (RF-03). Implemented by {@code DbListVehiclesByCustomer} in the data layer.
 */
public interface ListVehiclesByCustomer {

  List<Vehicle> list(Params params) throws UserNotFoundError;

  record Params(UUID customerId) {}
}
