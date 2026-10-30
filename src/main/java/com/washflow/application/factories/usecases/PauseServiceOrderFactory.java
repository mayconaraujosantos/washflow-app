package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.pauseserviceorder.DbPauseServiceOrder;
import com.washflow.domain.usecases.PauseServiceOrder;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class PauseServiceOrderFactory {

  private PauseServiceOrderFactory() {}

  public static PauseServiceOrder make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbPauseServiceOrder(repository);
  }
}
