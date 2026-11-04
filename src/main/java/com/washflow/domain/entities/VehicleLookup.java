package com.washflow.domain.entities;

import java.util.UUID;

/**
 * Read model for the manager's "search customer by plate/name/phone" box: a vehicle plus who owns
 * it - the registered customer when there is one, otherwise the walk-in contact.
 */
public record VehicleLookup(
    UUID vehicleId,
    String plate,
    String model,
    String color,
    UUID customerId,
    String customerName,
    String customerPhone) {}
