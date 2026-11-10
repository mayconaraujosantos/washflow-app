package com.washflow.data.protocols.db.cash;

import com.washflow.domain.entities.ExpenseCategory;

public interface SaveExpenseCategoryRepository {

  ExpenseCategory save(ExpenseCategory category);
}
