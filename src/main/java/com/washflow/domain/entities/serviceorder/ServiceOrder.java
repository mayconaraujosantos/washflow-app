package com.washflow.domain.entities.serviceorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code atendimentos} from CLAUDE.md - the operational queue. The id is generated here, in the
 * domain layer, rather than left to a DB identity/serial column, so any {@code
 * CreateServiceOrderRepository} implementation (JDBI today, Hibernate or anything else tomorrow)
 * just persists a value that already fully exists. {@code washerId} ({@code lavador_id}) is
 * nullable until a washer picks up the vehicle in {@code DbStartServiceOrder}.
 *
 * <p>{@code expectedDeliveryAt} ({@code previsao_entrega}) and {@code notes} ({@code observacoes})
 * are agreed with the customer when the manager registers the vehicle's entry; both are optional.
 *
 * <p>{@code servicePriceId} remains the first selected service for compatibility; {@code
 * servicePriceIds} holds the complete selection. {@code details} is read-only display data joined
 * from {@code veiculos}, {@code usuarios} and {@code servicos_preco} when the order is loaded. It
 * is {@code null} on orders that were just built in memory and not loaded back from the database.
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
    Instant expectedDeliveryAt,
    String notes,
    Details details,
    List<Integer> servicePriceIds) {

  public ServiceOrder {
    servicePriceIds =
        servicePriceIds == null || servicePriceIds.isEmpty()
            ? List.of(servicePriceId)
            : List.copyOf(servicePriceIds);
  }

  public ServiceOrder(
      UUID id,
      UUID vehicleId,
      int servicePriceId,
      UUID washerId,
      ServiceOrderStatus status,
      Instant scheduledAt,
      Instant createdAt,
      Instant updatedAt,
      String pauseReason,
      Instant expectedDeliveryAt,
      String notes,
      Details details) {
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
        expectedDeliveryAt,
        notes,
        details,
        List.of(servicePriceId));
  }

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
        null,
        null,
        null,
        List.of(servicePriceId));
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
    this(
        id,
        vehicleId,
        servicePriceId,
        washerId,
        status,
        scheduledAt,
        createdAt,
        updatedAt,
        null,
        null,
        null,
        null,
        List.of(servicePriceId));
  }

  public ServiceOrder(
      UUID id,
      UUID vehicleId,
      List<Integer> servicePriceIds,
      UUID washerId,
      ServiceOrderStatus status,
      Instant scheduledAt,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        vehicleId,
        servicePriceIds.getFirst(),
        washerId,
        status,
        scheduledAt,
        createdAt,
        updatedAt,
        null,
        null,
        null,
        null,
        servicePriceIds);
  }

  /** Display data for the order's vehicle, customer, selected services and assigned washer. */
  public record Details(
      String vehiclePlate,
      String vehicleModel,
      String vehicleColor,
      String customerName,
      String customerPhone,
      String serviceName,
      String washerName,
      BigDecimal serviceTotal) {
    public Details(
        String vehiclePlate,
        String vehicleModel,
        String vehicleColor,
        String customerName,
        String customerPhone,
        String serviceName,
        String washerName) {
      this(
          vehiclePlate,
          vehicleModel,
          vehicleColor,
          customerName,
          customerPhone,
          serviceName,
          washerName,
          null);
    }
  }
}
