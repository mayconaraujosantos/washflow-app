package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.scheduleserviceorder.DbScheduleServiceOrder;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the three {@code new *JdbiRepository(...)}
 * calls here - {@code DbScheduleServiceOrder} depends on the {@code data.protocols.db} interfaces,
 * not on these concrete classes.
 */
public final class ScheduleServiceOrderFactory {

  // docs/feature.spec.md 1.4: "Capacidade Máxima por Slot: Padrão de 3 veículos por hora
  // (configurável)" - the only one of the three MVP rules the spec calls out as configurable.
  private static final int DEFAULT_MAX_VEHICLES_PER_HOUR = 3;

  private ScheduleServiceOrderFactory() {}

  public static ScheduleServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbScheduleServiceOrder(
        new VehicleJdbiRepository(jdbi),
        new ServicePriceJdbiRepository(jdbi),
        serviceOrderRepository,
        serviceOrderRepository,
        Clock.systemUTC(),
        maxVehiclesPerHour());
  }

  private static int maxVehiclesPerHour() {
    String value = System.getenv("MAX_VEHICLES_PER_HOUR");
    return value == null || value.isBlank()
        ? DEFAULT_MAX_VEHICLES_PER_HOUR
        : Integer.parseInt(value);
  }
}
