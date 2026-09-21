package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code atendimentos} from CLAUDE.md - the operational queue. The id is generated here, in the
 * domain layer, rather than left to a DB identity/serial column, so any {@code
 * CreateServiceOrderRepository} implementation (JDBI today, Hibernate or anything else tomorrow)
 * just persists a value that already fully exists. {@code washerId} ({@code lavador_id}) is
 * nullable until a washer picks up the vehicle in {@code DbStartServiceOrder}.
 */
public record ServiceOrder(
    UUID id,
    UUID vehicleId,
    int servicePriceId,
    UUID washerId,
    ServiceOrderStatus status,
    Instant scheduledAt,
    Instant createdAt,
    Instant updatedAt) {}
