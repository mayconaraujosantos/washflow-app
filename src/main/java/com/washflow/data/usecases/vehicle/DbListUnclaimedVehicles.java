package com.washflow.data.usecases.vehicle;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.vehicle.ClaimVehicleRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.vehicle.ListUnclaimedVehicles;
import java.util.List;
import java.util.UUID;

public class DbListUnclaimedVehicles implements ListUnclaimedVehicles {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final ClaimVehicleRepository claimVehicleRepository;

  public DbListUnclaimedVehicles(
      LoadUserByIdRepository loadUserByIdRepository,
      ClaimVehicleRepository claimVehicleRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.claimVehicleRepository = claimVehicleRepository;
  }

  @Override
  public List<Vehicle> list(UUID customerId) throws UserNotFoundError {
    User customer =
        loadUserByIdRepository
            .loadById(customerId)
            .orElseThrow(() -> new UserNotFoundError(customerId));
    if (customer.profile() != UserProfile.CUSTOMER || customer.phone() == null) {
      return List.of();
    }
    return claimVehicleRepository.loadUnclaimedByContactPhone(customer.phone());
  }
}
