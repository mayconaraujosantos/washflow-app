package com.washflow.data.usecases.listexpensecategories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadExpenseCategoryRepository;
import com.washflow.domain.entities.ExpenseCategory;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListExpenseCategoriesTest {

  @Mock private LoadExpenseCategoryRepository loadRepository;

  private DbListExpenseCategories sut;

  @BeforeEach
  void setUp() {
    sut = new DbListExpenseCategories(loadRepository);
  }

  @Test
  void listsCategories() {
    List<ExpenseCategory> expected =
        List.of(
            new ExpenseCategory(UUID.randomUUID(), "Supplies", "#FF0000", Instant.now()),
            new ExpenseCategory(UUID.randomUUID(), "Utilities", "#00FF00", Instant.now()));
    when(loadRepository.loadAllCategories()).thenReturn(expected);

    List<ExpenseCategory> result = sut.list();

    assertEquals(2, result.size());
    verify(loadRepository).loadAllCategories();
  }

  @Test
  void returnsEmptyListWhenNoCategories() {
    when(loadRepository.loadAllCategories()).thenReturn(Collections.emptyList());

    List<ExpenseCategory> result = sut.list();

    assertEquals(0, result.size());
  }
}
