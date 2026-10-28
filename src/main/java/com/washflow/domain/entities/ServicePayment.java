package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServicePayment(
    UUID id, UUID serviceOrderId, PaymentMethod method, BigDecimal amount, Instant receivedAt) {}
