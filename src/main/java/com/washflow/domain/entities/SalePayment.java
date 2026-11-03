package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code venda_pagamentos}: one sale may be paid partially and with several methods. */
public record SalePayment(UUID id, PaymentMethod method, BigDecimal amount, Instant receivedAt) {}
