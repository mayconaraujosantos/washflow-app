package com.washflow.data.protocols.db.vehicle;

import com.washflow.domain.entities.vehicle.Vehicle;
import java.util.List;
import java.util.UUID;

/**
 * Vehicles the manager entered at the counter for someone without an account ({@code cliente_id}
 * NULL, contact phone kept instead) and handing one over to the customer who has that phone.
 */
public interface ClaimVehicleRepository {

  /** Counter vehicles without owner whose contact phone is {@code phone} (digits only). */
  List<Vehicle> loadUnclaimedByContactPhone(String phone);

  /**
   * Links the vehicle to the customer and drops the contact, keeping the stored model and color
   * when {@code model}/{@code color} are null. Compare-and-set: false when someone claimed it
   * first.
   */
  boolean claim(UUID vehicleId, UUID customerId, String model, String color);
}
