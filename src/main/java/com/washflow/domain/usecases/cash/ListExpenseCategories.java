package com.washflow.domain.usecases.cash;

import com.washflow.domain.entities.ExpenseCategory;
import java.util.List;

public interface ListExpenseCategories {

  List<ExpenseCategory> list();
}
