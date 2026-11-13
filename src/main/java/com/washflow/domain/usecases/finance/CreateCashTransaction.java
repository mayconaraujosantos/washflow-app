package com.washflow.domain.usecases.finance;

import com.washflow.domain.entities.finance.CashTransaction;
import com.washflow.domain.entities.finance.CashTransaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateCashTransaction {

  CashTransaction create(Params params);

  record Params(
      TransactionType type,
      BigDecimal amount,
      UUID categoryId,
      String description,
      LocalDate referenceDate,
      UUID serviceOrderId,
      UUID createdBy) {}
}
