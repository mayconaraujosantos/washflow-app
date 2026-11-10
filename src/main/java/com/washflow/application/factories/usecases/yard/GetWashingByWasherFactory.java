package com.washflow.application.factories.usecases.yard;

import com.washflow.data.usecases.yard.DbGetWashingByWasher;
import com.washflow.domain.usecases.yard.GetWashingByWasher;
import com.washflow.infra.db.jdbi.serviceorder.YardJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetWashingByWasherFactory {

  private GetWashingByWasherFactory() {}

  public static GetWashingByWasher make(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetWashingByWasher(repository);
  }
}
