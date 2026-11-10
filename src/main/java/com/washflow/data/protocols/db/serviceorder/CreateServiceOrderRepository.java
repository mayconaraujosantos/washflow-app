package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.time.Instant;
import java.util.Optional;

public interface CreateServiceOrderRepository {

  ServiceOrder create(ServiceOrder serviceOrder);

  /**
   * Creates a scheduled order only while its hour {@code [slotStart, slotEnd)} has fewer than
   * {@code capacity} bookings. Counting and inserting happen atomically per lava-jato and hour, so
   * simultaneous bookings can't overfill the slot. Empty when the slot is full.
   */
  Optional<ServiceOrder> createWithinCapacity(
      ServiceOrder serviceOrder, Instant slotStart, Instant slotEnd, int capacity);
}
