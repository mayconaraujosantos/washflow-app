package com.washflow.domain.usecases.whatsapp;

/**
 * Sends the WhatsApp reminder to customers whose appointment starts within the next hour, once per
 * appointment. Run periodically by the reminder job; implemented by {@code DbSendScheduleReminders}
 * in the data layer.
 */
public interface SendScheduleReminders {

  /** How many reminders this run claimed. */
  int sendDue();
}
