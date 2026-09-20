package com.washflow.domain.entities;

import java.util.UUID;

/**
 * {@code veiculos} from CLAUDE.md. {@code customerId} ({@code cliente_id}) is nullable - clients
 * without a full signup.
 */
public record Vehicle(UUID id, UUID customerId, String plate, String model, String color) {}
