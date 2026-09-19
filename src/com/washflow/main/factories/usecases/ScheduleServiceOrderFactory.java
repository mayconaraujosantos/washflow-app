package com.washflow.main.factories.usecases;

import com.washflow.data.usecases.scheduleserviceorder.DbScheduleServiceOrder;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the three {@code new *JdbiRepository(...)}
 * calls here - {@code DbScheduleServiceOrder} depends on the {@code data.protocols.db} interfaces,
 * not on these concrete classes.
 */
public final class ScheduleServiceOrderFactory {

  private ScheduleServiceOrderFactory() {}

  public static ScheduleServiceOrder make(Jdbi jdbi) {
    return new DbScheduleServiceOrder(
        new VehicleJdbiRepository(jdbi),
        new ServicePriceJdbiRepository(jdbi),
        new ServiceOrderJdbiRepository(jdbi));
  }
}
