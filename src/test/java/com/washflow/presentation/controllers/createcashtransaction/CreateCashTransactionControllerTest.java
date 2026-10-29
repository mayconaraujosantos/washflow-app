package com.washflow.presentation.controllers.createcashtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.usecases.CreateCashTransaction;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCashTransactionControllerTest {

  @Mock private CreateCashTransaction createCashTransaction;

  private CreateCashTransactionController sut;
  private UUID categoryId;
  private UUID createdBy;

  @BeforeEach
  void setUp() {
    sut = new CreateCashTransactionController(createCashTransaction);
    categoryId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
  }

  @Test
  void createsTransactionAndReturnsCreated() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "INCOME");
    body.put("amount", "150.00");
    body.put("referenceDate", "2025-01-15");
    body.put("categoryId", categoryId.toString());
    body.put("description", "Payment");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    CashTransaction saved =
        new CashTransaction(
            UUID.randomUUID(),
            TransactionType.INCOME,
            new BigDecimal("150.00"),
            categoryId,
            "Payment",
            LocalDate.of(2025, 1, 15),
            null,
            Instant.now(),
            createdBy);
    when(createCashTransaction.create(any(CreateCashTransaction.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
    verify(createCashTransaction).create(any(CreateCashTransaction.Params.class));
  }

  @Test
  void returnsBadRequestWhenTypeIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("amount", "100.00");
    body.put("referenceDate", "2025-01-15");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenAmountIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "INCOME");
    body.put("referenceDate", "2025-01-15");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenReferenceDateIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "INCOME");
    body.put("amount", "100.00");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenCreatedByIdIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "INCOME");
    body.put("amount", "100.00");
    body.put("referenceDate", "2025-01-15");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenInvalidType() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "INVALID");
    body.put("amount", "100.00");
    body.put("referenceDate", "2025-01-15");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
