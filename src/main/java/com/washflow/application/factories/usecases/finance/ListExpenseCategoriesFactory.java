package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbListExpenseCategories;
import com.washflow.domain.usecases.finance.ListExpenseCategories;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListExpenseCategoriesFactory {

  private ListExpenseCategoriesFactory() {}

  public static ListExpenseCategories make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListExpenseCategories(repository);
  }
}
