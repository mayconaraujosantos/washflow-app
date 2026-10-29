package com.washflow.presentation.controllers.listcashtransactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.usecases.ListCashTransactions;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCashTransactionsControllerTest {

  @Mock private ListCashTransactions listCashTransactions;

  private ListCashTransactionsController sut;

  @BeforeEach
  void setUp() {
    sut = new ListCashTransactionsController(listCashTransactions);
  }

  @Test
  void listsAllTransactionsWhenNoDateRange() {
    List<CashTransaction> transactions = Collections.emptyList();
    when(listCashTransactions.listAll()).thenReturn(transactions);
    HttpRequest request = new HttpRequest(Collections.emptyMap(), Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listCashTransactions).listAll();
  }

  @Test
  void listsTransactionsByDateRange() {
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 1, 31);
    List<CashTransaction> transactions =
        List.of(
            new CashTransaction(
                UUID.randomUUID(),
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                null,
                null,
                from,
                null,
                Instant.now(),
                null));
    when(listCashTransactions.list(eq(from), eq(to))).thenReturn(transactions);

    Map<String, Object> body = new HashMap<>();
    body.put("from", "2025-01-01");
    body.put("to", "2025-01-31");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listCashTransactions).list(from, to);
  }

  @Test
  void returnsBadRequestWhenInvalidDate() {
    Map<String, Object> body = new HashMap<>();
    body.put("from", "invalid");
    body.put("to", "2025-01-31");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
