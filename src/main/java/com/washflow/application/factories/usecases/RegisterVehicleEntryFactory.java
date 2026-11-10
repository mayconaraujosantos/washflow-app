package com.washflow.application.factories.usecases;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.registervehicleentry.DbRegisterVehicleEntry;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class RegisterVehicleEntryFactory {

  private RegisterVehicleEntryFactory() {}

  public static RegisterVehicleEntry make(Jdbi jdbi, AppConfig appConfig) {
    VehicleJdbiRepository vehicleRepository = new VehicleJdbiRepository(jdbi);
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbRegisterVehicleEntry(
        vehicleRepository,
        vehicleRepository,
        new UserJdbiRepository(jdbi),
        new ServicePriceJdbiRepository(jdbi),
        new SchedulingRules(serviceOrderRepository, EstablishmentHours.supplier(jdbi, appConfig)),
        serviceOrderRepository,
        serviceOrderRepository,
        Clock.systemUTC());
  }
}
