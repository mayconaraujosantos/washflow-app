package com.washflow.data.protocols.db.whatsapp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the WhatsApp notices need about a service order and its customer. */
public interface NotificationRepository {

  /**
   * The order's customer (account or counter contact) and car, in the current lava-jato; empty when
   * the order doesn't exist or has no phone to write to.
   */
  Optional<Recipient> recipientOf(UUID serviceOrderId);

  /**
   * Scheduled orders of every active lava-jato starting in {@code (from, to]} that have no reminder
   * yet and were booked at least two hours ahead. Runs outside any lava-jato (the reminder job).
   */
  List<DueReminder> dueReminders(Instant from, Instant to);

  /** Marks the reminder as sent, in the current lava-jato; false when another run already did. */
  boolean markReminded(UUID serviceOrderId, Instant at);

  record Recipient(
      String phone,
      String customerName,
      String vehicleModel,
      String vehiclePlate,
      String establishmentName,
      Instant scheduledAt) {}

  record DueReminder(UUID serviceOrderId, UUID establishmentId) {}
}
