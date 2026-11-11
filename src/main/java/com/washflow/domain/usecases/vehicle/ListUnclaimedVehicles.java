package com.washflow.domain.usecases.vehicle;

import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.user.UserNotFoundError;
import java.util.List;
import java.util.UUID;

/**
 * Cars the manager already entered at the counter with this customer's phone as the contact, so the
 * customer can add them with one tap ({@link CreateVehicle} with the same plate links them).
 */
public interface ListUnclaimedVehicles {

  List<Vehicle> list(UUID customerId) throws UserNotFoundError;
}
