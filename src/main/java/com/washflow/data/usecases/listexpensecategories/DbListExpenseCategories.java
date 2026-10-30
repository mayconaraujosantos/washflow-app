package com.washflow.data.usecases.listexpensecategories;

import com.washflow.data.protocols.db.LoadExpenseCategoryRepository;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.ListExpenseCategories;
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
