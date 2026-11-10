package com.washflow.domain.usecases.finance;

import com.washflow.domain.entities.finance.ExpenseCategory;
import java.util.List;

public interface ListExpenseCategories {

  List<ExpenseCategory> list();
}
