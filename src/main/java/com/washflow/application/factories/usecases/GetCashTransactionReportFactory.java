package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getcashtransactionreport.DbGetCashTransactionReport;
import com.washflow.domain.usecases.GetCashTransactionReport;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class GetCashTransactionReportFactory {

  private GetCashTransactionReportFactory() {}

  public static GetCashTransactionReport make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbGetCashTransactionReport(repository);
  }
}
