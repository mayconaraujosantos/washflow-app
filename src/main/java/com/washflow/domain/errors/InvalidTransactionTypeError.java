package com.washflow.domain.errors;

public class InvalidTransactionTypeError extends Exception {

  public InvalidTransactionTypeError(String value) {
    super("Invalid transaction type: " + value + ". Must be INCOME or EXPENSE");
  }
}
