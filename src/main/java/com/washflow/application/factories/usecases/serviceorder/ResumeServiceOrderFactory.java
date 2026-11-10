package com.washflow.application.factories.usecases.serviceorder;

import com.washflow.data.usecases.serviceorder.DbResumeServiceOrder;
import com.washflow.domain.usecases.serviceorder.ResumeServiceOrder;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ResumeServiceOrderFactory {

  private ResumeServiceOrderFactory() {}

  public static ResumeServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbResumeServiceOrder(repository);
  }
}
