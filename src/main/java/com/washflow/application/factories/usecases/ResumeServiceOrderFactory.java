package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.resumeserviceorder.DbResumeServiceOrder;
import com.washflow.domain.usecases.ResumeServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ResumeServiceOrderFactory {

  private ResumeServiceOrderFactory() {}

  public static ResumeServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbResumeServiceOrder(repository);
  }
}
