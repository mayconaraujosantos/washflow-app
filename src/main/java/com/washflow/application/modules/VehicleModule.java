package com.washflow.application.modules;

import com.washflow.data.usecases.vehicle.DbCreateVehicle;
import com.washflow.data.usecases.vehicle.DbListUnclaimedVehicles;
import com.washflow.data.usecases.vehicle.DbListVehiclesByCustomer;
import com.washflow.data.usecases.vehicle.DbSearchVehicles;
import com.washflow.domain.usecases.vehicle.CreateVehicle;
import com.washflow.domain.usecases.vehicle.ListUnclaimedVehicles;
import com.washflow.domain.usecases.vehicle.ListVehiclesByCustomer;
import com.washflow.domain.usecases.vehicle.SearchVehicles;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import com.washflow.presentation.controllers.vehicle.CreateVehicleController;
import com.washflow.presentation.controllers.vehicle.ListUnclaimedVehiclesController;
import com.washflow.presentation.controllers.vehicle.ListVehiclesByCustomerController;
import com.washflow.presentation.controllers.vehicle.SearchVehiclesController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** Composition root of vehicles: builds its use cases and controllers. */
public final class VehicleModule {

  private VehicleModule() {}

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)}
   * calls here - {@code DbCreateVehicle} depends on the {@code data.protocols.db} interfaces, not
   * on these concrete classes.
   */
  public static CreateVehicle createVehicle(Jdbi jdbi) {
    VehicleJdbiRepository vehicleRepository = new VehicleJdbiRepository(jdbi);
    return new DbCreateVehicle(
        new UserJdbiRepository(jdbi), vehicleRepository, vehicleRepository, vehicleRepository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)}
   * calls here - {@code DbListVehiclesByCustomer} depends on the {@code data.protocols.db}
   * interfaces, not on these concrete classes.
   */
  public static ListVehiclesByCustomer listVehiclesByCustomer(Jdbi jdbi) {
    return new DbListVehiclesByCustomer(
        new UserJdbiRepository(jdbi), new VehicleJdbiRepository(jdbi));
  }

  public static ListUnclaimedVehicles listUnclaimedVehicles(Jdbi jdbi) {
    return new DbListUnclaimedVehicles(
        new UserJdbiRepository(jdbi), new VehicleJdbiRepository(jdbi));
  }

  public static SearchVehicles searchVehicles(Jdbi jdbi) {
    return new DbSearchVehicles(new VehicleJdbiRepository(jdbi));
  }

  public static Controller createVehicleController(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("customerId"), new RequiredFieldValidation("plate")));

    return new CreateVehicleController(createVehicle(jdbi), validation);
  }

  public static Controller listVehiclesByCustomerController(Jdbi jdbi) {
    return new ListVehiclesByCustomerController(listVehiclesByCustomer(jdbi));
  }

  public static Controller listUnclaimedVehiclesController(Jdbi jdbi) {
    return new ListUnclaimedVehiclesController(listUnclaimedVehicles(jdbi));
  }

  public static Controller searchVehiclesController(Jdbi jdbi) {
    return new SearchVehiclesController(searchVehicles(jdbi));
  }
}
