package com.washflow.application.factories.usecases.serviceorder;

import com.washflow.data.usecases.serviceorder.DbCancelServiceOrder;
import com.washflow.domain.usecases.serviceorder.CancelServiceOrder;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CancelServiceOrderFactory {

  private CancelServiceOrderFactory() {}

  public static CancelServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbCancelServiceOrder(repository);
  }
}
