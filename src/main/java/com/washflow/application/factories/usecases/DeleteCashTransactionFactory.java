package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.deletecashtransaction.DbDeleteCashTransaction;
import com.washflow.domain.usecases.DeleteCashTransaction;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class DeleteCashTransactionFactory {

  private DeleteCashTransactionFactory() {}

  public static DeleteCashTransaction make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbDeleteCashTransaction(repository);
  }
}
