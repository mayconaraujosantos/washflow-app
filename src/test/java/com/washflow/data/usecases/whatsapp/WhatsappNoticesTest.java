package com.washflow.data.usecases.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.whatsapp.NotificationRepository;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.whatsapp.WhatsappInstance;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WhatsappNoticesTest {

  private static final UUID ESTABLISHMENT = UUID.randomUUID();
  private static final UUID ORDER = UUID.randomUUID();
  // 14:00 in Manaus (UTC-4).
  private static final Instant AT_TWO_PM = Instant.parse("2026-11-20T18:00:00Z");
  private static final NotificationRepository.Recipient ANA =
      new NotificationRepository.Recipient(
          "92991234567", "Ana Paula Souza", "Onix", "ABC1D23", "Lava Jato Central", AT_TWO_PM);

  private final NotificationRepository repository = mock(NotificationRepository.class);
  private final WhatsappGateway gateway = mock(WhatsappGateway.class);
  private final WhatsappNotices notices = new WhatsappNotices(repository, gateway, Runnable::run);

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void theReadyNoticeGoesFromTheLavaJatosNumberToTheCustomer() {
    when(repository.recipientOf(ORDER)).thenReturn(Optional.of(ANA));
    TenantContext.set(ESTABLISHMENT);

    notices.ready(ORDER);

    verify(gateway)
        .sendText(
            WhatsappInstance.nameOf(ESTABLISHMENT),
            "5592991234567",
            "Olá, Ana! Seu Onix (ABC1D23) está pronto para retirada no Lava Jato Central. 🚗✨");
  }

  @Test
  void theReminderSaysTheShopLocalTime() {
    String text = WhatsappNotices.reminderText(ANA);

    assertTrue(text.contains("é hoje às 14:00"), text);
    assertTrue(text.contains("*menu*"), text);
  }

  @Test
  void aProviderFailureNeverReachesTheWasher() {
    when(repository.recipientOf(ORDER)).thenReturn(Optional.of(ANA));
    doThrow(new WhatsappGatewayError("instance not connected"))
        .when(gateway)
        .sendText(anyString(), anyString(), anyString());
    TenantContext.set(ESTABLISHMENT);

    notices.ready(ORDER);
  }

  @Test
  void anOrderWithoutPhoneSendsNothing() {
    when(repository.recipientOf(ORDER)).thenReturn(Optional.empty());
    TenantContext.set(ESTABLISHMENT);

    notices.ready(ORDER);

    verify(gateway, never()).sendText(anyString(), anyString(), anyString());
  }

  @Test
  void eachReminderIsClaimedBeforeItIsSentAndSentOnce() {
    UUID claimedByAnotherRun = UUID.randomUUID();
    Instant now = Instant.parse("2026-11-20T17:00:00Z");
    when(repository.dueReminders(now, now.plus(DbSendScheduleReminders.AHEAD)))
        .thenReturn(
            List.of(
                new NotificationRepository.DueReminder(ORDER, ESTABLISHMENT),
                new NotificationRepository.DueReminder(claimedByAnotherRun, ESTABLISHMENT)));
    when(repository.markReminded(ORDER, now)).thenReturn(true);
    when(repository.markReminded(claimedByAnotherRun, now)).thenReturn(false);
    when(repository.recipientOf(ORDER)).thenReturn(Optional.of(ANA));

    int sent =
        new DbSendScheduleReminders(repository, notices, Clock.fixed(now, ZoneOffset.UTC))
            .sendDue();

    assertEquals(1, sent);
    verify(gateway)
        .sendText(eq(WhatsappInstance.nameOf(ESTABLISHMENT)), eq("5592991234567"), any());
    verify(repository, never()).recipientOf(claimedByAnotherRun);
    assertTrue(TenantContext.current().isEmpty());
  }
}
