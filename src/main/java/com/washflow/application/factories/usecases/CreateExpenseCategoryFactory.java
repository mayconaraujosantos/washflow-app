package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.createexpensecategory.DbCreateExpenseCategory;
import com.washflow.domain.usecases.CreateExpenseCategory;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class CreateExpenseCategoryFactory {

  private CreateExpenseCategoryFactory() {}

  public static CreateExpenseCategory make(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateExpenseCategory(repository);
  }
}
