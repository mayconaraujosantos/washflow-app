package com.washflow.data.usecases.createcashtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.SaveCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.errors.InvalidTransactionTypeError;
import com.washflow.domain.usecases.CreateCashTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbCreateCashTransactionTest {

  @Mock private SaveCashTransactionRepository saveRepository;

  private DbCreateCashTransaction sut;
  private UUID categoryId;
  private UUID createdBy;
  private LocalDate referenceDate;

  @BeforeEach
  void setUp() {
    sut = new DbCreateCashTransaction(saveRepository);
    categoryId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
    referenceDate = LocalDate.of(2025, 1, 15);
  }

  @Test
  void createsIncomeTransactionSuccessfully() throws Exception {
    CreateCashTransaction.Params params =
        new CreateCashTransaction.Params(
            TransactionType.INCOME,
            new BigDecimal("150.00"),
            categoryId,
            "Payment from client",
            referenceDate,
            null,
            createdBy);

    CashTransaction saved =
        new CashTransaction(
            UUID.randomUUID(),
            TransactionType.INCOME,
            new BigDecimal("150.00"),
            categoryId,
            "Payment from client",
            referenceDate,
            null,
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(CashTransaction.class))).thenReturn(saved);

    CashTransaction result = sut.create(params);

    assertNotNull(result);
    assertEquals(TransactionType.INCOME, result.type());
    assertEquals(new BigDecimal("150.00"), result.amount());
    verify(saveRepository).save(any(CashTransaction.class));
  }

  @Test
  void createsExpenseTransactionSuccessfully() throws Exception {
    CreateCashTransaction.Params params =
        new CreateCashTransaction.Params(
            TransactionType.EXPENSE,
            new BigDecimal("50.00"),
            categoryId,
            "Supplies",
            referenceDate,
            null,
            createdBy);

    CashTransaction saved =
        new CashTransaction(
            UUID.randomUUID(),
            TransactionType.EXPENSE,
            new BigDecimal("50.00"),
            categoryId,
            "Supplies",
            referenceDate,
            null,
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(CashTransaction.class))).thenReturn(saved);

    CashTransaction result = sut.create(params);

    assertEquals(TransactionType.EXPENSE, result.type());
    assertNotNull(result);
  }

  @Test
  void throwsInvalidTransactionTypeWhenTypeIsNull() {
    CreateCashTransaction.Params params =
        new CreateCashTransaction.Params(
            null, new BigDecimal("100.00"), categoryId, null, referenceDate, null, createdBy);

    assertThrows(InvalidTransactionTypeError.class, () -> sut.create(params));
    verifyNoInteractions(saveRepository);
  }

  @Test
  void createsTransactionWithServiceOrderId() throws Exception {
    UUID serviceOrderId = UUID.randomUUID();
    CreateCashTransaction.Params params =
        new CreateCashTransaction.Params(
            TransactionType.INCOME,
            new BigDecimal("200.00"),
            categoryId,
            null,
            referenceDate,
            serviceOrderId,
            createdBy);

    CashTransaction saved =
        new CashTransaction(
            UUID.randomUUID(),
            TransactionType.INCOME,
            new BigDecimal("200.00"),
            categoryId,
            null,
            referenceDate,
            serviceOrderId,
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(CashTransaction.class))).thenReturn(saved);

    CashTransaction result = sut.create(params);

    assertEquals(serviceOrderId, result.serviceOrderId());
  }
}
