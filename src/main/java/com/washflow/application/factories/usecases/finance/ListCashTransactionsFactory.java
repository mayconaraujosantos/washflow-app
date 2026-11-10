package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbListCashTransactions;
import com.washflow.domain.usecases.finance.ListCashTransactions;
import com.washflow.infra.db.jdbi.finance.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListCashTransactionsFactory {

  private ListCashTransactionsFactory() {}

  public static ListCashTransactions make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListCashTransactions(repository);
  }
}
