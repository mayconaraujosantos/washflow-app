package com.washflow.data.protocols.db.cash;

import com.washflow.domain.entities.ExpenseCategory;
import java.util.List;

public interface LoadExpenseCategoryRepository {

  List<ExpenseCategory> loadAllCategories();
}
