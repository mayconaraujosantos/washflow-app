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

  /** {@code housekeeping} (old received-message ids) runs on the same thread, after each run. */
  public static void start(SendScheduleReminders reminders, Runnable housekeeping) {
    ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "schedule-reminders");
              thread.setDaemon(true);
              return thread;
            });
    scheduler.scheduleWithFixedDelay(
        () -> {
          run(reminders);
          runHousekeeping(housekeeping);
        },
        1,
        PERIOD_MINUTES,
        TimeUnit.MINUTES);
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

  private static void runHousekeeping(Runnable housekeeping) {
    try {
      housekeeping.run();
    } catch (RuntimeException e) {
      LOGGER.error("event=whatsapp_housekeeping_failed", e);
    }
  }
}
