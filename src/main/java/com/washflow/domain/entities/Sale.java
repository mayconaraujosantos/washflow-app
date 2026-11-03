package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code vendas}: a quick counter sale or a budget, with catalog items, an optional discount and
 * zero or more payments. {@code paidAmount}/{@code outstandingAmount} are derived from the payments
 * and kept as components so they are part of the API response.
 */
public record Sale(
    UUID id,
    SaleType type,
    UUID vehicleId,
    String customerName,
    String customerPhone,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String notes,
    UUID createdBy,
    Instant createdAt,
    List<SaleItem> items,
    List<SalePayment> payments,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount) {

  public Sale {
    items = List.copyOf(items);
    payments = List.copyOf(payments);
  }

  @SuppressWarnings("java:S107") // mirrors the vendas columns
  public static Sale of(
      UUID id,
      SaleType type,
      UUID vehicleId,
      String customerName,
      String customerPhone,
      BigDecimal discount,
      String notes,
      UUID createdBy,
      Instant createdAt,
      List<SaleItem> items,
      List<SalePayment> payments) {
    BigDecimal subtotal =
        items.stream().map(SaleItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal total = subtotal.subtract(discount);
    BigDecimal paid =
        payments.stream().map(SalePayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new Sale(
        id,
        type,
        vehicleId,
        customerName,
        customerPhone,
        subtotal,
        discount,
        total,
        notes,
        createdBy,
        createdAt,
        items,
        payments,
        paid,
        total.subtract(paid));
  }
}
