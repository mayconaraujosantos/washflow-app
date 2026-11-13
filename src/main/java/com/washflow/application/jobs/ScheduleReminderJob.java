package com.washflow.application.jobs;

import com.washflow.domain.usecases.whatsapp.SendScheduleReminders;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every five minutes, sends the WhatsApp reminder of appointments starting within the hour. Safe
 * with several replicas: each reminder is claimed in the database before it is sent.
 */
public final class ScheduleReminderJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleReminderJob.class);
  private static final long PERIOD_MINUTES = 5;

  private ScheduleReminderJob() {}

  public static void start(SendScheduleReminders reminders) {
    ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "schedule-reminders");
              thread.setDaemon(true);
              return thread;
            });
    scheduler.scheduleWithFixedDelay(() -> run(reminders), 1, PERIOD_MINUTES, TimeUnit.MINUTES);
  }

  static void run(SendScheduleReminders reminders) {
    // An exception escaping here would cancel every later run of the scheduler.
    try {
      int sent = reminders.sendDue();
      if (sent > 0) {
        LOGGER.info("event=schedule_reminders_sent count={}", sent);
      }
    } catch (RuntimeException e) {
      LOGGER.error("event=schedule_reminders_failed", e);
    }
  }
}
