package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.createvehicle.DbCreateVehicle;
import com.washflow.domain.usecases.CreateVehicle;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)} calls
 * here - {@code DbCreateVehicle} depends on the {@code data.protocols.db} interfaces, not on these
 * concrete classes.
 */
public final class CreateVehicleFactory {

  private CreateVehicleFactory() {}

  public static CreateVehicle make(Jdbi jdbi) {
    VehicleJdbiRepository vehicleRepository = new VehicleJdbiRepository(jdbi);
    return new DbCreateVehicle(new UserJdbiRepository(jdbi), vehicleRepository, vehicleRepository);
  }
}
