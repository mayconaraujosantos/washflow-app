package com.washflow.infra.db.jdbi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue and commissions come from payments received in the period; refunds are counted on the day
 * they were made, so a closed day never changes. {@code netRevenue = revenue - refunds} and {@code
 * netCommissions = commissions - reversedCommissions}.
 */
public record OperationsReport(
    LocalDate from,
    LocalDate to,
    int finalizedOrders,
    BigDecimal revenue,
    BigDecimal commissions,
    Long averageWashingMinutes,
    List<ServiceBreakdown> services,
    List<PaymentBreakdown> paymentMethods,
    List<PauseBreakdown> pauses,
    int refundedOrders,
    BigDecimal refunds,
    BigDecimal reversedCommissions,
    BigDecimal netRevenue,
    BigDecimal netCommissions,
    List<RefundBreakdown> refundReasons) {

  public OperationsReport(
      LocalDate from,
      LocalDate to,
      int finalizedOrders,
      BigDecimal revenue,
      BigDecimal commissions,
      Long averageWashingMinutes,
      List<ServiceBreakdown> services,
      List<PaymentBreakdown> paymentMethods,
      List<PauseBreakdown> pauses,
      int refundedOrders,
      BigDecimal refunds,
      BigDecimal reversedCommissions,
      List<RefundBreakdown> refundReasons) {
    this(
        from,
        to,
        finalizedOrders,
        revenue,
        commissions,
        averageWashingMinutes,
        services,
        paymentMethods,
        pauses,
        refundedOrders,
        refunds,
        reversedCommissions,
        revenue.subtract(refunds),
        commissions.subtract(reversedCommissions),
        refundReasons);
  }

  /** A period without refunds. */
  public OperationsReport(
      LocalDate from,
      LocalDate to,
      int finalizedOrders,
      BigDecimal revenue,
      BigDecimal commissions,
      Long averageWashingMinutes,
      List<ServiceBreakdown> services,
      List<PaymentBreakdown> paymentMethods,
      List<PauseBreakdown> pauses) {
    this(
        from,
        to,
        finalizedOrders,
        revenue,
        commissions,
        averageWashingMinutes,
        services,
        paymentMethods,
        pauses,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        List.of());
  }

  public record ServiceBreakdown(
      String serviceName, int orders, BigDecimal revenue, BigDecimal commissions) {}

  public record PaymentBreakdown(String method, int orders, BigDecimal amount) {}

  public record PauseBreakdown(String reason, int count) {}

  public record RefundBreakdown(String reason, int count, BigDecimal amount) {}
}
