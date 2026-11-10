package com.washflow.data.usecases.finance;

import com.washflow.data.protocols.db.finance.LoadCashTransactionRepository;
import com.washflow.data.protocols.db.finance.UpdateCashTransactionRepository;
import com.washflow.domain.entities.finance.CashTransaction;
import com.washflow.domain.usecases.finance.UpdateCashTransaction;
import java.util.UUID;

public class DbUpdateCashTransaction implements UpdateCashTransaction {

  private final LoadCashTransactionRepository loadRepository;
  private final UpdateCashTransactionRepository updateRepository;

  public DbUpdateCashTransaction(
      LoadCashTransactionRepository loadRepository,
      UpdateCashTransactionRepository updateRepository) {
    this.loadRepository = loadRepository;
    this.updateRepository = updateRepository;
  }

  @Override
  public CashTransaction update(UUID id, CashTransaction.PartialUpdate update) {
    CashTransaction existing = loadRepository.loadById(id);

    CashTransaction merged =
        new CashTransaction(
            existing.id(),
            update.type() != null ? update.type() : existing.type(),
            update.amount() != null ? update.amount() : existing.amount(),
            update.categoryId() != null ? update.categoryId() : existing.categoryId(),
            update.description() != null ? update.description() : existing.description(),
            update.referenceDate() != null ? update.referenceDate() : existing.referenceDate(),
            update.serviceOrderId() != null ? update.serviceOrderId() : existing.serviceOrderId(),
            existing.createdAt(),
            existing.createdBy());

    return updateRepository.update(merged);
  }
}
