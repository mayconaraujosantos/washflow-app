package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.data.usecases.yard.DbGetWaitingQueue;
import com.washflow.data.usecases.yard.DbGetWashingByWasher;
import com.washflow.data.usecases.yard.DbGetYardOverview;
import com.washflow.data.usecases.yard.DbRegisterVehicleEntry;
import com.washflow.domain.usecases.yard.GetWaitingQueue;
import com.washflow.domain.usecases.yard.GetWashingByWasher;
import com.washflow.domain.usecases.yard.GetYardOverview;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import com.washflow.presentation.controllers.yard.GetWaitingQueueController;
import com.washflow.presentation.controllers.yard.GetWashingByWasherController;
import com.washflow.presentation.controllers.yard.GetYardOverviewController;
import com.washflow.presentation.controllers.yard.RegisterVehicleEntryController;
import com.washflow.presentation.protocols.Controller;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the yard: builds its use cases and controllers. */
public final class YardModule {

  private YardModule() {}

  public static GetWaitingQueue getWaitingQueue(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetWaitingQueue(repository);
  }

  public static GetWashingByWasher getWashingByWasher(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetWashingByWasher(repository);
  }

  public static GetYardOverview getYardOverview(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetYardOverview(repository);
  }

  public static RegisterVehicleEntry registerVehicleEntry(Jdbi jdbi, AppConfig appConfig) {
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

  public static Controller getWaitingQueueController(Jdbi jdbi) {
    return new GetWaitingQueueController(getWaitingQueue(jdbi));
  }

  public static Controller getWashingByWasherController(Jdbi jdbi) {
    return new GetWashingByWasherController(getWashingByWasher(jdbi));
  }

  public static Controller getYardOverviewController(Jdbi jdbi) {
    return new GetYardOverviewController(getYardOverview(jdbi));
  }

  public static Controller registerVehicleEntryController(Jdbi jdbi, AppConfig appConfig) {
    return new RegisterVehicleEntryController(registerVehicleEntry(jdbi, appConfig));
  }
}
