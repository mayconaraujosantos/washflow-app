package com.washflow.presentation.controllers.updatecashtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.PartialUpdate;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.usecases.UpdateCashTransaction;
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
class UpdateCashTransactionControllerTest {

  @Mock private UpdateCashTransaction updateCashTransaction;

  private UpdateCashTransactionController sut;
  private UUID transactionId;

  @BeforeEach
  void setUp() {
    sut = new UpdateCashTransactionController(updateCashTransaction);
    transactionId = UUID.randomUUID();
  }

  @Test
  void updatesTransactionAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("amount", "200.00");
    body.put("description", "Updated");
    HttpRequest request = new HttpRequest(body, Map.of("id", transactionId.toString()), Map.of());

    CashTransaction updated =
        new CashTransaction(
            transactionId,
            TransactionType.INCOME,
            new BigDecimal("200.00"),
            null,
            "Updated",
            LocalDate.of(2025, 1, 15),
            null,
            Instant.now(),
            null);
    when(updateCashTransaction.update(eq(transactionId), any(PartialUpdate.class)))
        .thenReturn(updated);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(updateCashTransaction).update(eq(transactionId), any(PartialUpdate.class));
  }

  @Test
  void returnsBadRequestWhenInvalidId() {
    Map<String, Object> body = new HashMap<>();
    body.put("amount", "200.00");
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenInvalidAmount() {
    Map<String, Object> body = new HashMap<>();
    body.put("amount", "not-a-number");
    HttpRequest request = new HttpRequest(body, Map.of("id", transactionId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
