package com.washflow.application.factories.usecases.vehicle;

import com.washflow.data.usecases.vehicle.DbListVehiclesByCustomer;
import com.washflow.domain.usecases.vehicle.ListVehiclesByCustomer;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)} calls
 * here - {@code DbListVehiclesByCustomer} depends on the {@code data.protocols.db} interfaces, not
 * on these concrete classes.
 */
public final class ListVehiclesByCustomerFactory {

  private ListVehiclesByCustomerFactory() {}

  public static ListVehiclesByCustomer make(Jdbi jdbi) {
    return new DbListVehiclesByCustomer(
        new UserJdbiRepository(jdbi), new VehicleJdbiRepository(jdbi));
  }
}
