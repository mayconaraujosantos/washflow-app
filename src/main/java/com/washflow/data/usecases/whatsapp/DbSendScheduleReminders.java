package com.washflow.data.usecases.whatsapp;

import com.washflow.data.protocols.db.whatsapp.NotificationRepository;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.usecases.whatsapp.SendScheduleReminders;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Claims each due reminder (compare-and-set on {@code lembrete_enviado_em}) before sending it, so
 * overlapping runs or several replicas send it once; a send that fails after the claim is not
 * retried - better one missed reminder than a customer reminded twice.
 */
public class DbSendScheduleReminders implements SendScheduleReminders {

  static final Duration AHEAD = Duration.ofHours(1);

  private final NotificationRepository repository;
  private final WhatsappNotices notices;
  private final Clock clock;

  public DbSendScheduleReminders(
      NotificationRepository repository, WhatsappNotices notices, Clock clock) {
    this.repository = repository;
    this.notices = notices;
    this.clock = clock;
  }

  @Override
  public int sendDue() {
    Instant now = Instant.now(clock);
    int sent = 0;
    for (NotificationRepository.DueReminder due : repository.dueReminders(now, now.plus(AHEAD))) {
      boolean claimed =
          TenantContext.runAs(
              due.establishmentId(),
              () -> {
                if (!repository.markReminded(due.serviceOrderId(), now)) {
                  return false;
                }
                notices.reminder(due.establishmentId(), due.serviceOrderId());
                return true;
              });
      if (claimed) {
        sent++;
      }
    }
    return sent;
  }
}
