package com.washflow.application.factories.usecases.yard;

import com.washflow.application.AppConfig;
import com.washflow.application.factories.usecases.establishment.EstablishmentHours;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.data.usecases.yard.DbRegisterVehicleEntry;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
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
        new SchedulingRules(
            new AgendaJdbiRepository(jdbi), EstablishmentHours.supplier(jdbi, appConfig)),
        serviceOrderRepository,
        serviceOrderRepository,
        Clock.systemUTC());
  }
}
