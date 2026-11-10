package com.washflow.domain.usecases;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Refunds all or part of a quick sale in one payment method. Returns the sale with its refunds.
 * Implemented by {@code DbRefundSale} in the data layer.
 */
public interface RefundSale {

  Sale refund(Params params) throws SaleNotFoundError, SaleRefundNotAllowedError;

  record Params(
      UUID saleId,
      UUID managerId,
      PaymentMethod method,
      BigDecimal amount,
      String reason,
      String description) {}
}
