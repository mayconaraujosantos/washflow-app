package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.updatecashtransaction.DbUpdateCashTransaction;
import com.washflow.domain.usecases.UpdateCashTransaction;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class UpdateCashTransactionFactory {

  private UpdateCashTransactionFactory() {}

  public static UpdateCashTransaction make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbUpdateCashTransaction(repository, repository);
  }
}
