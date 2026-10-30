package com.washflow.data.usecases.updatecashtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadCashTransactionRepository;
import com.washflow.data.protocols.db.UpdateCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.PartialUpdate;
import com.washflow.domain.entities.CashTransaction.TransactionType;
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
class DbUpdateCashTransactionTest {

  @Mock private LoadCashTransactionRepository loadRepository;
  @Mock private UpdateCashTransactionRepository updateRepository;

  private DbUpdateCashTransaction sut;
  private UUID transactionId;
  private CashTransaction existingTransaction;

  @BeforeEach
  void setUp() {
    sut = new DbUpdateCashTransaction(loadRepository, updateRepository);
    transactionId = UUID.randomUUID();
    existingTransaction =
        new CashTransaction(
            transactionId,
            TransactionType.INCOME,
            new BigDecimal("100.00"),
            UUID.randomUUID(),
            "Original",
            LocalDate.of(2025, 1, 15),
            null,
            Instant.now(),
            null);
  }

  @Test
  void updatesAmountOnly() {
    when(loadRepository.loadById(transactionId)).thenReturn(existingTransaction);
    when(updateRepository.update(any(CashTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

    PartialUpdate update =
        new PartialUpdate(null, new BigDecimal("200.00"), null, null, null, null);
    CashTransaction result = sut.update(transactionId, update);

    assertEquals(new BigDecimal("200.00"), result.amount());
    assertEquals(TransactionType.INCOME, result.type());
    assertEquals("Original", result.description());
  }

  @Test
  void updatesDescriptionOnly() {
    when(loadRepository.loadById(transactionId)).thenReturn(existingTransaction);
    when(updateRepository.update(any(CashTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

    PartialUpdate update = new PartialUpdate(null, null, null, "Updated", null, null);
    CashTransaction result = sut.update(transactionId, update);

    assertEquals("Updated", result.description());
    assertEquals(new BigDecimal("100.00"), result.amount());
  }

  @Test
  void preservesCreatedAtAndCreatedBy() {
    when(loadRepository.loadById(transactionId)).thenReturn(existingTransaction);
    when(updateRepository.update(any(CashTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

    PartialUpdate update = new PartialUpdate(TransactionType.EXPENSE, null, null, null, null, null);
    CashTransaction result = sut.update(transactionId, update);

    assertEquals(existingTransaction.createdAt(), result.createdAt());
    assertEquals(existingTransaction.createdBy(), result.createdBy());
  }
}
