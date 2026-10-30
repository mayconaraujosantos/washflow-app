package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getwashingbywasher.DbGetWashingByWasher;
import com.washflow.domain.usecases.GetWashingByWasher;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetWashingByWasherFactory {

  private GetWashingByWasherFactory() {}

  public static GetWashingByWasher make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbGetWashingByWasher(repository);
  }
}
