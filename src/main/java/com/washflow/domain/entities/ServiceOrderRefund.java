package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The full refund of a finalized order ({@code estornos_atendimento}): a new entry dated when it
 * was made, never a change to the original payment. {@code reversedCommission} is zero when the
 * washer keeps the commission.
 */
public record ServiceOrderRefund(
    UUID id,
    UUID serviceOrderId,
    BigDecimal amount,
    PaymentMethod method,
    String reason,
    String description,
    BigDecimal reversedCommission,
    UUID createdBy,
    Instant createdAt) {}
