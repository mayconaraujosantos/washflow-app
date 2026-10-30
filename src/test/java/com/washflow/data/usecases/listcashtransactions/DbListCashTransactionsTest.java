package com.washflow.data.usecases.listcashtransactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListCashTransactionsTest {

  @Mock private LoadCashTransactionRepository loadRepository;

  private DbListCashTransactions sut;

  @BeforeEach
  void setUp() {
    sut = new DbListCashTransactions(loadRepository);
  }

  @Test
  void listByDateRange() {
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 1, 31);
    List<CashTransaction> expected =
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
    when(loadRepository.loadByDateRange(eq(from), eq(to))).thenReturn(expected);

    List<CashTransaction> result = sut.list(from, to);

    assertEquals(1, result.size());
    verify(loadRepository).loadByDateRange(from, to);
  }

  @Test
  void listAll() {
    List<CashTransaction> expected = Collections.emptyList();
    when(loadRepository.loadAll()).thenReturn(expected);

    List<CashTransaction> result = sut.listAll();

    assertEquals(0, result.size());
    verify(loadRepository).loadAll();
  }
}
