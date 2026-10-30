package com.washflow.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A washer's commission credited for a finished {@code atendimento}, computed from {@code
 * servicos_preco.comissao_lavador} when {@code DbFinalizeServiceOrder} closes the sale. Not yet in
 * CLAUDE.md's dictionary - the manager's cash-out/commission history flow is new - so this would
 * back a {@code comissoes_lavador} table. The id is generated here, in the domain layer, same as
 * {@link ServiceOrder}.
 */
public record WasherCommission(
    UUID id, UUID serviceOrderId, UUID washerId, BigDecimal amount, Instant creditedAt) {}
