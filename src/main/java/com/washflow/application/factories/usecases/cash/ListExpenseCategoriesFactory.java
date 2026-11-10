package com.washflow.application.factories.usecases.cash;

import com.washflow.data.usecases.cash.DbListExpenseCategories;
import com.washflow.domain.usecases.cash.ListExpenseCategories;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListExpenseCategoriesFactory {

  private ListExpenseCategoriesFactory() {}

  public static ListExpenseCategories make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListExpenseCategories(repository);
  }
}
