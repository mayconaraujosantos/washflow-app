package com.washflow.domain.usecases.finance;

import com.washflow.domain.entities.ExpenseCategory;

public interface CreateExpenseCategory {

  ExpenseCategory create(Params params);

  record Params(String name, String color) {}
}
