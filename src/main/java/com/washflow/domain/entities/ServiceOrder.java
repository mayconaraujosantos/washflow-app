package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code atendimentos} from CLAUDE.md - the operational queue. The id is generated here, in the
 * domain layer, rather than left to a DB identity/serial column, so any {@code
 * CreateServiceOrderRepository} implementation (JDBI today, Hibernate or anything else tomorrow)
 * just persists a value that already fully exists. {@code washerId} ({@code lavador_id}) is
 * nullable until a washer picks up the vehicle in {@code DbStartServiceOrder}.
 *
 * <p>{@code details} is read-only display data joined from {@code veiculos}, {@code usuarios} and
 * {@code servicos_preco} when the order is loaded, so the yard/queue screens can show the plate and
 * service without one extra request per card. It is {@code null} on orders that were just built in
 * memory and not loaded back from the database.
 */
public record ServiceOrder(
    UUID id,
    UUID vehicleId,
    int servicePriceId,
    UUID washerId,
    ServiceOrderStatus status,
    Instant scheduledAt,
    Instant createdAt,
    Instant updatedAt,
    String pauseReason,
    Details details) {

  public ServiceOrder(
      UUID id,
      UUID vehicleId,
      int servicePriceId,
      UUID washerId,
      ServiceOrderStatus status,
      Instant scheduledAt,
      Instant createdAt,
      Instant updatedAt,
      String pauseReason) {
    this(
        id,
        vehicleId,
        servicePriceId,
        washerId,
        status,
        scheduledAt,
        createdAt,
        updatedAt,
        pauseReason,
        null);
  }

  public ServiceOrder(
      UUID id,
      UUID vehicleId,
      int servicePriceId,
      UUID washerId,
      ServiceOrderStatus status,
      Instant scheduledAt,
      Instant createdAt,
      Instant updatedAt) {
    this(id, vehicleId, servicePriceId, washerId, status, scheduledAt, createdAt, updatedAt, null);
  }

  /** Display data for the order's vehicle, customer, service and assigned washer. */
  public record Details(
      String vehiclePlate,
      String vehicleModel,
      String vehicleColor,
      String customerName,
      String serviceName,
      String washerName) {}
}
