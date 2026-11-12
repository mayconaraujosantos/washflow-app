package com.washflow.domain.usecases.sale;

import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.shared.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Refunds all or part of a quick sale in one payment method. Returns the sale with its refunds.
 * Implemented by {@code DbRefundSale} in the data layer.
 */
public interface RefundSale {

  Sale refund(Params params);

  record Params(
      UUID saleId,
      UUID managerId,
      PaymentMethod method,
      BigDecimal amount,
      String reason,
      String description) {}
}
