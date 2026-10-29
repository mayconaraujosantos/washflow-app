package com.washflow.presentation.controllers.listexpensecategories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.ListExpenseCategories;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListExpenseCategoriesControllerTest {

  @Mock private ListExpenseCategories listExpenseCategories;

  private ListExpenseCategoriesController sut;

  @BeforeEach
  void setUp() {
    sut = new ListExpenseCategoriesController(listExpenseCategories);
  }

  @Test
  void returnsCategoriesWhenFound() {
    List<ExpenseCategory> categories =
        List.of(
            new ExpenseCategory(UUID.randomUUID(), "Supplies", "#FF0000", Instant.now()),
            new ExpenseCategory(UUID.randomUUID(), "Utilities", "#00FF00", Instant.now()));
    when(listExpenseCategories.list()).thenReturn(categories);
    HttpRequest request = new HttpRequest(Collections.emptyMap(), Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listExpenseCategories).list();
  }

  @Test
  void returnsEmptyListWhenNoCategories() {
    when(listExpenseCategories.list()).thenReturn(Collections.emptyList());
    HttpRequest request = new HttpRequest(Collections.emptyMap(), Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
