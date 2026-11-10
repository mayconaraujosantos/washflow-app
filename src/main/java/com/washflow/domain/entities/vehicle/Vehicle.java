package com.washflow.domain.entities.vehicle;

import java.util.UUID;

/**
 * {@code veiculos} from CLAUDE.md. {@code customerId} ({@code cliente_id}) is nullable - clients
 * without a full signup. For those, {@code contactName}/{@code contactPhone} keep who to call when
 * the car is ready (filled when the manager registers a walk-in entry).
 */
public record Vehicle(
    UUID id,
    UUID customerId,
    String plate,
    String model,
    String color,
    String contactName,
    String contactPhone) {

  public Vehicle(UUID id, UUID customerId, String plate, String model, String color) {
    this(id, customerId, plate, model, color, null, null);
  }
}
