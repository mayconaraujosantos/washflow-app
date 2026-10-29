package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getoperationsreport.DbGetOperationsReport;
import com.washflow.domain.usecases.GetOperationsReport;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetOperationsReportFactory {

  private GetOperationsReportFactory() {}

  public static GetOperationsReport make(Jdbi jdbi) {
    ServiceOrderJdbiRepository repository = new ServiceOrderJdbiRepository(jdbi);
    return new DbGetOperationsReport(repository);
  }
}
