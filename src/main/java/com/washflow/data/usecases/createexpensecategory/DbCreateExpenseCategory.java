package com.washflow.data.usecases.createexpensecategory;

import com.washflow.data.protocols.db.SaveExpenseCategoryRepository;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.CreateExpenseCategory;
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
