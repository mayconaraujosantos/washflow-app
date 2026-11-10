package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code vendas}: a quick counter sale or a budget, with catalog items, an optional discount and
 * zero or more payments and refunds. {@code paidAmount}/{@code outstandingAmount} are derived from
 * the payments and {@code refundedAmount} from the refunds; all are kept as components so they are
 * part of the API response. Refunds never change {@code paidAmount}.
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
    BigDecimal outstandingAmount,
    List<SaleRefund> refunds,
    BigDecimal refundedAmount) {

  public Sale {
    items = List.copyOf(items);
    payments = List.copyOf(payments);
    refunds = List.copyOf(refunds);
  }

  /** A sale without refunds (just created). */
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
    return of(
        id,
        type,
        vehicleId,
        customerName,
        customerPhone,
        discount,
        notes,
        createdBy,
        createdAt,
        items,
        payments,
        List.of());
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
      List<SalePayment> payments,
      List<SaleRefund> refunds) {
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
        total.subtract(paid),
        refunds,
        refunds.stream().map(SaleRefund::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
  }
}
