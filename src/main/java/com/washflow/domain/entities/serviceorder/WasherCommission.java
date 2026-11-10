package com.washflow.domain.entities.serviceorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A washer's commission credited for a finished {@code atendimento}, computed as the sum of {@code
 * servicos_preco.comissao_lavador} for its selected services when {@code DbFinalizeServiceOrder}
 * closes the sale. The id is generated here, in the domain layer, same as {@link ServiceOrder}.
 */
public record WasherCommission(
    UUID id, UUID serviceOrderId, UUID washerId, BigDecimal amount, Instant creditedAt) {}
