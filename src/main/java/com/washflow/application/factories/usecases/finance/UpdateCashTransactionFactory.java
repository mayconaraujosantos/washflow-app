package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbUpdateCashTransaction;
import com.washflow.domain.usecases.finance.UpdateCashTransaction;
import com.washflow.infra.db.jdbi.finance.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class UpdateCashTransactionFactory {

  private UpdateCashTransactionFactory() {}

  public static UpdateCashTransaction make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbUpdateCashTransaction(repository, repository);
  }
}
