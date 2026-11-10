package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.refundserviceorder.DbRefundServiceOrder;
import com.washflow.domain.usecases.RefundServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class RefundServiceOrderFactory {

  private RefundServiceOrderFactory() {}

  public static RefundServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbRefundServiceOrder(repository);
  }
}
