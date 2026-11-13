package com.washflow.data.usecases.vehicle;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.vehicle.ClaimVehicleRepository;
import com.washflow.data.protocols.db.vehicle.CreateVehicleRepository;
import com.washflow.data.protocols.db.vehicle.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
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
  private final ClaimVehicleRepository claimVehicleRepository;

  public DbCreateVehicle(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadVehicleByPlateRepository loadVehicleByPlateRepository,
      CreateVehicleRepository createVehicleRepository,
      ClaimVehicleRepository claimVehicleRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadVehicleByPlateRepository = loadVehicleByPlateRepository;
    this.createVehicleRepository = createVehicleRepository;
    this.claimVehicleRepository = claimVehicleRepository;
  }

  @Override
  public Vehicle create(Params params) {
    User customer =
        loadUserByIdRepository
            .loadById(params.customerId())
            .orElseThrow(() -> new UserNotFoundError(params.customerId()));
    String plate = Vehicle.normalizePlate(params.plate());
    String model = ContactRules.limited(params.model(), Vehicle.MAX_MODEL, "Modelo");
    String color = ContactRules.limited(params.color(), Vehicle.MAX_COLOR, "Cor");

    var existing = loadVehicleByPlateRepository.loadByPlate(plate);
    if (existing.isPresent()) {
      Vehicle vehicle = existing.get();
      if (enteredAtCounterFor(vehicle, customer)
          && claimVehicleRepository.claim(vehicle.id(), customer.id(), model, color)) {
        return new Vehicle(
            vehicle.id(),
            customer.id(),
            plate,
            model != null ? model : vehicle.model(),
            color != null ? color : vehicle.color());
      }
      throw new VehiclePlateAlreadyRegisteredError(plate);
    }

    return createVehicleRepository.create(
        new Vehicle(UUID.randomUUID(), params.customerId(), plate, model, color));
  }

  /**
   * Walk-in car whose contact phone is this customer's - the same trust the counter entry already
   * gives a phone when it links a car to an existing account (DbRegisterVehicleEntry).
   */
  private static boolean enteredAtCounterFor(Vehicle vehicle, User customer) {
    return vehicle.customerId() == null
        && customer.profile() == UserProfile.CUSTOMER
        && customer.phone() != null
        && customer.phone().equals(vehicle.contactPhone());
  }
}
