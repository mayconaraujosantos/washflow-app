package com.washflow.data.usecases.finance;

import com.washflow.data.protocols.db.finance.LoadExpenseCategoryRepository;
import com.washflow.domain.entities.finance.ExpenseCategory;
import com.washflow.domain.usecases.finance.ListExpenseCategories;
import java.util.List;

public class DbListExpenseCategories implements ListExpenseCategories {

  private final LoadExpenseCategoryRepository loadRepository;

  public DbListExpenseCategories(LoadExpenseCategoryRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ExpenseCategory> list() {
    return loadRepository.loadAllCategories();
  }
}
