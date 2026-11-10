package com.washflow.domain.entities.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue and commissions come from payments received in the period; refunds and cancellations are
 * counted on the day they were made, so a closed day never changes. {@code netRevenue = revenue -
 * refunds}, {@code netCommissions = commissions - reversedCommissions} and {@code netSales =
 * salesReceived - saleRefunds} (quick sales, by payment and refund date).
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
    List<RefundBreakdown> refundReasons,
    int cancelledOrders,
    List<ReasonCount> cancellationReasons,
    BigDecimal salesReceived,
    BigDecimal saleRefunds,
    BigDecimal netSales,
    List<RefundBreakdown> saleRefundReasons) {

  /** A period with no refunds, cancellations or quick sales. */
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
        revenue,
        commissions,
        List.of(),
        0,
        List.of(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        List.of());
  }

  public record ServiceBreakdown(
      String serviceName, int orders, BigDecimal revenue, BigDecimal commissions) {}

  public record PaymentBreakdown(String method, int orders, BigDecimal amount) {}

  public record PauseBreakdown(String reason, int count) {}

  public record RefundBreakdown(String reason, int count, BigDecimal amount) {}

  public record ReasonCount(String reason, int count) {}
}
