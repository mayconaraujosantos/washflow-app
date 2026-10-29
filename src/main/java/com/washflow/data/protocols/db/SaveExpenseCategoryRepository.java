package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ExpenseCategory;

public interface SaveExpenseCategoryRepository {

  ExpenseCategory save(ExpenseCategory category);
}
