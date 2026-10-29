package com.washflow.presentation.controllers.createexpensecategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.usecases.CreateExpenseCategory;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateExpenseCategoryControllerTest {

  @Mock private CreateExpenseCategory createExpenseCategory;

  private CreateExpenseCategoryController sut;

  @BeforeEach
  void setUp() {
    sut = new CreateExpenseCategoryController(createExpenseCategory);
  }

  @Test
  void createsCategoryAndReturnsCreated() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "Supplies");
    body.put("color", "#FF0000");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    ExpenseCategory saved =
        new ExpenseCategory(UUID.randomUUID(), "Supplies", "#FF0000", Instant.now());
    when(createExpenseCategory.create(any(CreateExpenseCategory.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
    verify(createExpenseCategory).create(any(CreateExpenseCategory.Params.class));
  }

  @Test
  void usesDefaultColorWhenMissing() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "Utilities");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    ExpenseCategory saved =
        new ExpenseCategory(UUID.randomUUID(), "Utilities", "#6B7280", Instant.now());
    when(createExpenseCategory.create(any(CreateExpenseCategory.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenNameIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("color", "#FF0000");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenNameIsBlank() {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "  ");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
