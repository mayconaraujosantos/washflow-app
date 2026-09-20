package com.washflow.domain.entities;

import java.math.BigDecimal;

/** {@code servicos_preco} from CLAUDE.md - the wash type catalog. */
public record ServicePrice(int id, String name, BigDecimal price, BigDecimal washerCommission) {}
