package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

public record ExpenseCategory(UUID id, String name, String color, Instant createdAt) {}
