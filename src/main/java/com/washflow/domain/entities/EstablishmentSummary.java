package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/** A lava-jato as the platform administrator sees it. */
public record EstablishmentSummary(
    UUID id,
    String slug,
    String name,
    boolean active,
    Instant createdAt,
    String managerName,
    String managerEmail) {}
