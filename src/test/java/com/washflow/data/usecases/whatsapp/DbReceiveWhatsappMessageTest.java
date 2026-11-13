package com.washflow.data.usecases.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.whatsapp.ConversationRepository;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.whatsapp.Conversation;
import com.washflow.domain.entities.whatsapp.WhatsappInstance;
import com.washflow.domain.usecases.whatsapp.ReceiveWhatsappMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DbReceiveWhatsappMessageTest {

  private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
  private static final UUID ESTABLISHMENT = UUID.randomUUID();
  private static final String INSTANCE = WhatsappInstance.nameOf(ESTABLISHMENT);
  // Registered on WhatsApp without the ninth digit.
  private static final String SENDER = "559291234567";
  private static final User ANA =
      new User(UUID.randomUUID(), "Ana", "92991234567", UserProfile.CUSTOMER, NOW, true);

  private final ConversationRepository conversations = mock(ConversationRepository.class);
  private final LoadUserByPhoneRepository users = mock(LoadUserByPhoneRepository.class);
  private final WhatsappAssistant assistant = mock(WhatsappAssistant.class);
  private final WhatsappGateway gateway = mock(WhatsappGateway.class);
  private DbReceiveWhatsappMessage receive;

  @BeforeEach
  void setUp() {
    receive =
        new DbReceiveWhatsappMessage(
            conversations, users, assistant, gateway, Clock.fixed(NOW, ZoneOffset.UTC));
    when(conversations.activeEstablishmentName()).thenReturn(Optional.of("Lava Jato Central"));
    when(conversations.markReceived(anyString(), any())).thenReturn(true);
    when(conversations.load(SENDER)).thenReturn(Optional.empty());
    when(users.loadByPhone(anyString())).thenReturn(Optional.empty());
    when(users.loadByPhone("92991234567")).thenReturn(Optional.of(ANA));
  }

  @Test
  void answersInsideTheLavaJatoOfTheInstance() {
    Conversation next = Conversation.fresh(NOW);
    when(assistant.reply(any(), any(), eq("oi"), eq(NOW)))
        .thenAnswer(
            call -> {
              assertEquals(Optional.of(ESTABLISHMENT), TenantContext.current());
              return new WhatsappAssistant.Turn(List.of("Olá!", "Menu"), next, ANA);
            });

    receive.receive(new ReceiveWhatsappMessage.Params(INSTANCE, "m1", SENDER, "oi"));

    ArgumentCaptor<WhatsappAssistant.Contact> contact =
        ArgumentCaptor.forClass(WhatsappAssistant.Contact.class);
    verify(assistant).reply(contact.capture(), any(), any(), any());
    // The account is found by the phone with the ninth digit.
    assertEquals(ANA, contact.getValue().customer());
    assertEquals("92991234567", contact.getValue().phone());
    verify(conversations).save(SENDER, next);
    verify(gateway).sendText(INSTANCE, SENDER, "Olá!");
    verify(gateway).sendText(INSTANCE, SENDER, "Menu");
    assertTrue(TenantContext.current().isEmpty());
  }

  @Test
  void aResentWebhookIsHandledOnce() {
    when(conversations.markReceived(eq("m1"), any())).thenReturn(false);

    receive.receive(new ReceiveWhatsappMessage.Params(INSTANCE, "m1", SENDER, "1"));

    verifyNoInteractions(assistant, gateway);
  }

  @Test
  void anAudioGetsTheTextOnlyAnswer() {
    receive.receive(new ReceiveWhatsappMessage.Params(INSTANCE, "m1", SENDER, null));

    verify(gateway).sendText(INSTANCE, SENDER, DbReceiveWhatsappMessage.TEXT_ONLY);
    verifyNoInteractions(assistant);
  }

  @Test
  void aSuspendedLavaJatoOrAnotherInstanceStaysQuiet() {
    receive.receive(new ReceiveWhatsappMessage.Params("washflow", "m1", SENDER, "oi"));
    when(conversations.activeEstablishmentName()).thenReturn(Optional.empty());
    receive.receive(new ReceiveWhatsappMessage.Params(INSTANCE, "m2", SENDER, "oi"));

    verifyNoInteractions(assistant, gateway);
    verify(conversations, never()).markReceived(anyString(), any());
  }

  @Test
  void aFailureStillAnswersTheCustomer() {
    when(assistant.reply(any(), any(), any(), any())).thenThrow(new IllegalStateException("db"));

    receive.receive(new ReceiveWhatsappMessage.Params(INSTANCE, "m1", SENDER, "1"));

    verify(gateway).sendText(INSTANCE, SENDER, DbReceiveWhatsappMessage.TRY_AGAIN);
    verify(conversations, never()).save(anyString(), any());
  }
}
