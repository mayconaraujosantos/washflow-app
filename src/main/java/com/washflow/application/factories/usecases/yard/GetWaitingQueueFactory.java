package com.washflow.application.factories.usecases.yard;

import com.washflow.data.usecases.yard.DbGetWaitingQueue;
import com.washflow.domain.usecases.yard.GetWaitingQueue;
import com.washflow.infra.db.jdbi.YardJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetWaitingQueueFactory {

  private GetWaitingQueueFactory() {}

  public static GetWaitingQueue make(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetWaitingQueue(repository);
  }
}
