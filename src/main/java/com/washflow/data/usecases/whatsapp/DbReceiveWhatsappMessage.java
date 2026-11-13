package com.washflow.data.usecases.whatsapp;

import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.whatsapp.ConversationRepository;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.whatsapp.WhatsappInstance;
import com.washflow.domain.entities.whatsapp.WhatsappNumber;
import com.washflow.domain.usecases.whatsapp.ReceiveWhatsappMessage;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the assistant inside the lava-jato the instance belongs to. A message is handled once (the
 * provider resends webhooks), and one phone's messages are handled one at a time, so two quick
 * messages never read the same conversation step.
 */
public class DbReceiveWhatsappMessage implements ReceiveWhatsappMessage {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbReceiveWhatsappMessage.class);
  private static final int LOCK_STRIPES = 64;
  static final String TRY_AGAIN = "Tive um problema agora. Tente de novo em instantes.";
  static final String TEXT_ONLY =
      "Por enquanto entendo só mensagens de texto. Envie *menu* para ver as opções.";

  private final ConversationRepository conversations;
  private final LoadUserByPhoneRepository users;
  private final WhatsappAssistant assistant;
  private final WhatsappGateway gateway;
  private final Clock clock;
  private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

  public DbReceiveWhatsappMessage(
      ConversationRepository conversations,
      LoadUserByPhoneRepository users,
      WhatsappAssistant assistant,
      WhatsappGateway gateway,
      Clock clock) {
    this.conversations = conversations;
    this.users = users;
    this.assistant = assistant;
    this.gateway = gateway;
    this.clock = clock;
    for (int i = 0; i < LOCK_STRIPES; i++) {
      locks[i] = new ReentrantLock();
    }
  }

  @Override
  public void receive(Params params) {
    Optional<UUID> establishment = WhatsappInstance.establishmentOf(params.instance());
    if (establishment.isEmpty() || params.whatsappNumber() == null) {
      return;
    }
    TenantContext.runAs(
        establishment.get(),
        () -> {
          handle(params);
          return null;
        });
  }

  private void handle(Params params) {
    Optional<String> establishmentName = conversations.activeEstablishmentName();
    if (establishmentName.isEmpty()) {
      return;
    }
    Instant now = Instant.now(clock);
    if (!conversations.markReceived(params.messageId(), now)) {
      return;
    }
    String number = params.whatsappNumber();
    if (params.text() == null || params.text().isBlank()) {
      send(params.instance(), number, List.of(TEXT_ONLY));
      return;
    }
    ReentrantLock lock = locks[Math.floorMod(number.hashCode(), LOCK_STRIPES)];
    lock.lock();
    try {
      reply(params, establishmentName.get(), now);
    } catch (RuntimeException e) {
      // The message is already marked as received: answer, or the customer waits forever.
      LOGGER.error("event=whatsapp_assistant_failed", e);
      send(params.instance(), number, List.of(TRY_AGAIN));
    } finally {
      lock.unlock();
    }
  }

  private void reply(Params params, String establishmentName, Instant now) {
    String number = params.whatsappNumber();
    List<String> candidates = WhatsappNumber.phoneCandidates(number);
    WhatsappAssistant.Turn turn =
        assistant.reply(
            new WhatsappAssistant.Contact(
                establishmentName, preferredPhone(candidates), account(candidates)),
            conversations.load(number).orElse(null),
            params.text(),
            now);
    conversations.save(number, turn.next());
    send(params.instance(), number, turn.messages());
  }

  /** The account of any of the phone's forms (with or without the ninth digit). */
  private User account(List<String> candidates) {
    for (String phone : candidates) {
      Optional<User> user = users.loadByPhone(phone);
      if (user.isPresent()) {
        return user.get();
      }
    }
    return null;
  }

  /** A new account keeps the mobile ninth digit, as the app's forms do. */
  private static String preferredPhone(List<String> candidates) {
    return candidates.stream()
        .filter(phone -> phone.length() == 11)
        .findFirst()
        .orElse(candidates.isEmpty() ? null : candidates.get(0));
  }

  private void send(String instance, String number, List<String> messages) {
    try {
      for (String message : messages) {
        gateway.sendText(instance, number, message);
      }
    } catch (WhatsappGatewayError e) {
      LOGGER.error("event=whatsapp_reply_failed reason={}", e.getMessage(), e);
    }
  }
}
