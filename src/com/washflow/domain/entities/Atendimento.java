package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code atendimentos} from CLAUDE.md - the operational queue. The id is generated here, in the
 * domain layer, rather than left to a DB identity/serial column, so any {@code
 * CriarAtendimentoRepository} implementation (JDBI today, Hibernate or anything else tomorrow) just
 * persists a value that already fully exists.
 */
public record Atendimento(
    UUID id,
    UUID veiculoId,
    int servicoPrecoId,
    StatusAtendimento status,
    Instant dataAgendamento,
    Instant criadoEm,
    Instant atualizadoEm) {}
