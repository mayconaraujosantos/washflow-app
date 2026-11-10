package com.washflow.domain.errors.cash;

public class InvalidTransactionTypeError extends Exception {

  public InvalidTransactionTypeError(String value) {
    super("Invalid transaction type: " + value + ". Must be INCOME or EXPENSE");
  }
}
