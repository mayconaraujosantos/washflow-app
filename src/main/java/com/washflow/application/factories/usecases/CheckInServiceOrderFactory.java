package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.checkinserviceorder.DbCheckInServiceOrder;
import com.washflow.domain.usecases.CheckInServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ServiceOrderJdbiRepository(...)} call here - {@code DbCheckInServiceOrder} depends on the {@code
 * data.protocols.db} interfaces, not on this concrete class.
 */
public final class CheckInServiceOrderFactory {

  private CheckInServiceOrderFactory() {}

  public static CheckInServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCheckInServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }
}
