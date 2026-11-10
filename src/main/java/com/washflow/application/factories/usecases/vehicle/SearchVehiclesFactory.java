package com.washflow.application.factories.usecases.vehicle;

import com.washflow.data.usecases.vehicle.DbSearchVehicles;
import com.washflow.domain.usecases.vehicle.SearchVehicles;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class SearchVehiclesFactory {

  private SearchVehiclesFactory() {}

  public static SearchVehicles make(Jdbi jdbi) {
    return new DbSearchVehicles(new VehicleJdbiRepository(jdbi));
  }
}
