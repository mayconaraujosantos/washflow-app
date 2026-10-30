package com.washflow.domain.usecases;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.errors.InvalidTransactionTypeError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateCashTransaction {

  CashTransaction create(Params params) throws InvalidTransactionTypeError;

  record Params(
      TransactionType type,
      BigDecimal amount,
      UUID categoryId,
      String description,
      LocalDate referenceDate,
      UUID serviceOrderId,
      UUID createdBy) {}
}
