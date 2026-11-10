package com.washflow.domain.entities.sale;

import com.washflow.domain.entities.shared.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code venda_estornos}: money given back on a quick sale, in one method, up to what came in
 * through that method. A sale may have several (partial) refunds; payments are never changed.
 */
public record SaleRefund(
    UUID id,
    UUID saleId,
    PaymentMethod method,
    BigDecimal amount,
    String reason,
    String description,
    UUID createdBy,
    Instant createdAt) {}
