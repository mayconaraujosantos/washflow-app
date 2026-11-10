package com.washflow.application.factories.usecases.finance;

import com.washflow.data.usecases.finance.DbCreateExpenseCategory;
import com.washflow.domain.usecases.finance.CreateExpenseCategory;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CreateExpenseCategoryFactory {

  private CreateExpenseCategoryFactory() {}

  public static CreateExpenseCategory make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateExpenseCategory(repository);
  }
}
