package com.washflow.infra.db.jdbi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationsReport(
    LocalDate from,
    LocalDate to,
    int finalizedOrders,
    BigDecimal revenue,
    BigDecimal commissions,
    Long averageWashingMinutes,
    List<ServiceBreakdown> services,
    List<PaymentBreakdown> paymentMethods,
    List<PauseBreakdown> pauses) {

  public record ServiceBreakdown(
      String serviceName, int orders, BigDecimal revenue, BigDecimal commissions) {}

  public record PaymentBreakdown(String method, int orders, BigDecimal amount) {}

  public record PauseBreakdown(String reason, int count) {}
}
