package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listcashtransactions.DbListCashTransactions;
import com.washflow.domain.usecases.ListCashTransactions;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListCashTransactionsFactory {

  private ListCashTransactionsFactory() {}

  public static ListCashTransactions make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListCashTransactions(repository);
  }
}
