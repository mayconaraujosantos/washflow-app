package com.washflow.application.factories.usecases.yard;

import com.washflow.data.usecases.yard.DbGetYardOverview;
import com.washflow.domain.usecases.yard.GetYardOverview;
import com.washflow.infra.db.jdbi.YardJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetYardOverviewFactory {

  private GetYardOverviewFactory() {}

  public static GetYardOverview make(Jdbi jdbi) {
    YardJdbiRepository repository = new YardJdbiRepository(jdbi);
    return new DbGetYardOverview(repository);
  }
}
