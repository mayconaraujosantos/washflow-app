package com.washflow.data.usecases.createexpensecategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.SaveExpenseCategoryRepository;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.CreateExpenseCategory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbCreateExpenseCategoryTest {

  @Mock private SaveExpenseCategoryRepository saveRepository;

  private DbCreateExpenseCategory sut;

  @BeforeEach
  void setUp() {
    sut = new DbCreateExpenseCategory(saveRepository);
  }

  @Test
  void createsCategorySuccessfully() {
    CreateExpenseCategory.Params params = new CreateExpenseCategory.Params("Supplies", "#FF0000");

    ExpenseCategory saved =
        new ExpenseCategory(UUID.randomUUID(), "Supplies", "#FF0000", Instant.now());
    when(saveRepository.save(any(ExpenseCategory.class))).thenReturn(saved);

    ExpenseCategory result = sut.create(params);

    assertNotNull(result);
    assertEquals("Supplies", result.name());
    assertEquals("#FF0000", result.color());
    verify(saveRepository).save(any(ExpenseCategory.class));
  }

  @Test
  void trimsName() {
    CreateExpenseCategory.Params params =
        new CreateExpenseCategory.Params("  Cleaning  ", "#00FF00");

    ExpenseCategory saved =
        new ExpenseCategory(UUID.randomUUID(), "Cleaning", "#00FF00", Instant.now());
    when(saveRepository.save(any(ExpenseCategory.class))).thenReturn(saved);

    sut.create(params);

    verify(saveRepository).save(any(ExpenseCategory.class));
  }
}
