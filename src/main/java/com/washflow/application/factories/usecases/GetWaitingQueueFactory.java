package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getwaitingqueue.DbGetWaitingQueue;
import com.washflow.domain.usecases.GetWaitingQueue;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetWaitingQueueFactory {

  private GetWaitingQueueFactory() {}

  public static GetWaitingQueue make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbGetWaitingQueue(repository);
  }
}
