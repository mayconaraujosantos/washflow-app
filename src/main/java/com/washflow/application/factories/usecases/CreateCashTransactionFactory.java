package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.createcashtransaction.DbCreateCashTransaction;
import com.washflow.domain.usecases.CreateCashTransaction;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CreateCashTransactionFactory {

  private CreateCashTransactionFactory() {}

  public static CreateCashTransaction make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateCashTransaction(repository);
  }
}
