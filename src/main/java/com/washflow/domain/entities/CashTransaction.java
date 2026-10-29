package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashTransaction(
    UUID id,
    TransactionType type,
    BigDecimal amount,
    UUID categoryId,
    String description,
    LocalDate referenceDate,
    UUID serviceOrderId,
    Instant createdAt,
    UUID createdBy) {

  public enum TransactionType {
    INCOME,
    EXPENSE
  }
}
