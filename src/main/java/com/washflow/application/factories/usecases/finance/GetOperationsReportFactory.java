package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbGetOperationsReport;
import com.washflow.domain.usecases.finance.GetOperationsReport;
import com.washflow.infra.db.jdbi.finance.OperationsReportJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetOperationsReportFactory {

  private GetOperationsReportFactory() {}

  public static GetOperationsReport make(Jdbi jdbi) {
    OperationsReportJdbiRepository repository = new OperationsReportJdbiRepository(jdbi);
    return new DbGetOperationsReport(repository);
  }
}
