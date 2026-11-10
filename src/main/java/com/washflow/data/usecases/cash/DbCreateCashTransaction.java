package com.washflow.data.usecases.cash;

import com.washflow.data.protocols.db.cash.SaveCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.errors.cash.InvalidTransactionTypeError;
import com.washflow.domain.usecases.cash.CreateCashTransaction;
import java.time.Instant;
import java.util.UUID;

public class DbCreateCashTransaction implements CreateCashTransaction {

  private final SaveCashTransactionRepository saveRepository;

  public DbCreateCashTransaction(SaveCashTransactionRepository saveRepository) {
    this.saveRepository = saveRepository;
  }

  @Override
  public CashTransaction create(Params params) throws InvalidTransactionTypeError {
    if (params.type() == null) {
      throw new InvalidTransactionTypeError(null);
    }

    CashTransaction transaction =
        new CashTransaction(
            UUID.randomUUID(),
            params.type(),
            params.amount(),
            params.categoryId(),
            params.description(),
            params.referenceDate(),
            params.serviceOrderId(),
            Instant.now(),
            params.createdBy());

    return saveRepository.save(transaction);
  }
}
