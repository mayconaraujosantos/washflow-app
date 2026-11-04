package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code venda_itens}: catalog name and price are frozen at sale time. */
public record SaleItem(
    UUID id,
    int servicePriceId,
    String description,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal total) {}
