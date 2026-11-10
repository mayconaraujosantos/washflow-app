package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbCreateCashTransaction;
import com.washflow.domain.usecases.finance.CreateCashTransaction;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CreateCashTransactionFactory {

  private CreateCashTransactionFactory() {}

  public static CreateCashTransaction make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateCashTransaction(repository);
  }
}
