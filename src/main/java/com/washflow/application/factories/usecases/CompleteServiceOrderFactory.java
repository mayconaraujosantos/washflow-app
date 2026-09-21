package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.completeserviceorder.DbCompleteServiceOrder;
import com.washflow.domain.usecases.CompleteServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ServiceOrderJdbiRepository(...)} call here - {@code DbCompleteServiceOrder} depends on the {@code
 * data.protocols.db} interfaces, not on this concrete class.
 */
public final class CompleteServiceOrderFactory {

  private CompleteServiceOrderFactory() {}

  public static CompleteServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCompleteServiceOrder(serviceOrderRepository, serviceOrderRepository);
  }
}
