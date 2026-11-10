package com.washflow.data.protocols.db.finance;

import com.washflow.domain.entities.finance.ExpenseCategory;

public interface SaveExpenseCategoryRepository {

  ExpenseCategory save(ExpenseCategory category);
}
