package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.cancelserviceorder.DbCancelServiceOrder;
import com.washflow.domain.usecases.serviceorder.CancelServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CancelServiceOrderFactory {

  private CancelServiceOrderFactory() {}

  public static CancelServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCancelServiceOrder(repository);
  }
}
