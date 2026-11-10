package com.washflow.data.usecases.finance;

import com.washflow.data.protocols.db.finance.SaveExpenseCategoryRepository;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.finance.CreateExpenseCategory;
import java.time.Instant;
import java.util.UUID;

public class DbCreateExpenseCategory implements CreateExpenseCategory {

  private final SaveExpenseCategoryRepository saveRepository;

  public DbCreateExpenseCategory(SaveExpenseCategoryRepository saveRepository) {
    this.saveRepository = saveRepository;
  }

  @Override
  public ExpenseCategory create(Params params) {
    ExpenseCategory category =
        new ExpenseCategory(UUID.randomUUID(), params.name().trim(), params.color(), Instant.now());
    return saveRepository.save(category);
  }
}
