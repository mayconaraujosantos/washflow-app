package com.washflow.domain.entities.serviceorder;

import java.time.Instant;

/**
 * What the public ticket page shows: vehicle, selected services and progress of one order.
 * Deliberately no customer name or phone - anyone holding the ticket link can open it.
 */
public record TicketView(
    String establishmentName,
    String vehiclePlate,
    String vehicleModel,
    String serviceName,
    ServiceOrderStatus status,
    String pauseReason,
    Instant scheduledAt,
    Instant expectedDeliveryAt,
    Instant updatedAt) {}
