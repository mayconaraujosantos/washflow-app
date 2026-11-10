package com.washflow.domain.entities.sale;

import com.washflow.domain.entities.shared.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code venda_pagamentos}: one sale may be paid partially and with several methods. */
public record SalePayment(UUID id, PaymentMethod method, BigDecimal amount, Instant receivedAt) {}
