package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.finalizeserviceorder.DbFinalizeServiceOrder;
import com.washflow.domain.usecases.FinalizeServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.WasherCommissionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)} calls
 * here - {@code DbFinalizeServiceOrder} depends on the {@code data.protocols.db} interfaces, not on
 * these concrete classes.
 */
public final class FinalizeServiceOrderFactory {

  private FinalizeServiceOrderFactory() {}

  public static FinalizeServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository serviceOrderRepository = new ServiceOrderJdbiRepository(jdbi);
    return new DbFinalizeServiceOrder(
        serviceOrderRepository,
        new ServicePriceJdbiRepository(jdbi),
        serviceOrderRepository,
        new WasherCommissionJdbiRepository(jdbi));
  }
}
