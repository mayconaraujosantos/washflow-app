package com.washflow.data.protocols.db.finance;

import com.washflow.domain.entities.finance.ExpenseCategory;
import java.util.List;

public interface LoadExpenseCategoryRepository {

  List<ExpenseCategory> loadAllCategories();
}
