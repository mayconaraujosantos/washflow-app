package com.washflow.domain.errors.finance;

import com.washflow.domain.errors.common.ValidationError;

public class InvalidTransactionTypeError extends ValidationError {

  public InvalidTransactionTypeError(String value) {
    super(
        "INVALID_TRANSACTION_TYPE",
        "Invalid transaction type: " + value + ". Must be INCOME or EXPENSE");
  }
}
