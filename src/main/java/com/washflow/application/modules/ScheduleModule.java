package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.schedule.DbListAgenda;
import com.washflow.data.usecases.schedule.DbScheduleServiceOrder;
import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.domain.usecases.schedule.ScheduleServiceOrder;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import com.washflow.presentation.controllers.schedule.ListAgendaController;
import com.washflow.presentation.controllers.schedule.ScheduleServiceOrderController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.time.Clock;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the agenda: builds its use cases and controllers. */
public final class ScheduleModule {

  private ScheduleModule() {}

  public static ListAgenda listAgenda(Jdbi jdbi) {
    return new DbListAgenda(new AgendaJdbiRepository(jdbi));
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the three {@code new
   * *JdbiRepository(...)} calls here - {@code DbScheduleServiceOrder} depends on the {@code
   * data.protocols.db} interfaces, not on these concrete classes.
   */
  public static ScheduleServiceOrder scheduleServiceOrder(Jdbi jdbi, AppConfig appConfig) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbScheduleServiceOrder(
        new VehicleJdbiRepository(jdbi),
        new ServicePriceJdbiRepository(jdbi),
        new AgendaJdbiRepository(jdbi),
        serviceOrderRepository,
        Clock.systemUTC(),
        EstablishmentHours.supplier(jdbi, appConfig));
  }

  public static Controller listAgendaController(Jdbi jdbi) {
    return new ListAgendaController(listAgenda(jdbi));
  }

  public static Controller scheduleServiceOrderController(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("vehicleId"),
                new RequiredFieldValidation("scheduledAt")));

    return new ScheduleServiceOrderController(scheduleServiceOrder(jdbi, appConfig), validation);
  }
}
