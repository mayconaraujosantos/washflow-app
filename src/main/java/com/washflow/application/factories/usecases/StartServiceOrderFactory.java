package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.startserviceorder.DbStartServiceOrder;
import com.washflow.domain.usecases.StartServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ServiceOrderJdbiRepository(...)} call here - {@code DbStartServiceOrder} depends on the {@code
 * data.protocols.db} interfaces, not on this concrete class.
 */
public final class StartServiceOrderFactory {

  private StartServiceOrderFactory() {}

  public static StartServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbStartServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }
}
