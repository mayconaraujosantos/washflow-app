package com.washflow.domain.entities;

import java.util.UUID;

/**
 * {@code veiculos} from CLAUDE.md. {@code clienteId} is nullable - clients without a full signup.
 */
public record Veiculo(UUID id, UUID clienteId, String placa, String modelo, String cor) {}
