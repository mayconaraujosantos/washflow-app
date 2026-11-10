package com.washflow.application.factories.usecases.serviceorder;

import com.washflow.data.usecases.serviceorder.DbRefundServiceOrder;
import com.washflow.domain.usecases.serviceorder.RefundServiceOrder;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class RefundServiceOrderFactory {

  private RefundServiceOrderFactory() {}

  public static RefundServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbRefundServiceOrder(repository);
  }
}
