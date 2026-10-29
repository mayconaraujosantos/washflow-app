package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getyardoverview.DbGetYardOverview;
import com.washflow.domain.usecases.GetYardOverview;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetYardOverviewFactory {

  private GetYardOverviewFactory() {}

  public static GetYardOverview make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbGetYardOverview(repository);
  }
}
