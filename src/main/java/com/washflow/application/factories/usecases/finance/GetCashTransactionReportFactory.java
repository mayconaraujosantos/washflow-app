package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbGetCashTransactionReport;
import com.washflow.domain.usecases.finance.GetCashTransactionReport;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetCashTransactionReportFactory {

  private GetCashTransactionReportFactory() {}

  public static GetCashTransactionReport make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbGetCashTransactionReport(repository);
  }
}
