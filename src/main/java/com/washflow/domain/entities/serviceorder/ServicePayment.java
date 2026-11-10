package com.washflow.domain.entities.serviceorder;

import com.washflow.domain.entities.shared.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServicePayment(
    UUID id, UUID serviceOrderId, PaymentMethod method, BigDecimal amount, Instant receivedAt) {}
