package com.washflow.data.protocols.db.whatsapp;

import com.washflow.domain.entities.whatsapp.Conversation;
import java.time.Instant;
import java.util.Optional;

/** The assistant's memory, inside the current lava-jato. */
public interface ConversationRepository {

  /** The lava-jato's name, or empty when it doesn't exist or is suspended. */
  Optional<String> activeEstablishmentName();

  Optional<Conversation> load(String whatsappNumber);

  void save(String whatsappNumber, Conversation conversation);

  /**
   * Records a received message id; false when it was already recorded (a webhook resent by the
   * provider), so the message is handled once.
   */
  boolean markReceived(String messageId, Instant at);

  /** Forgets received ids older than {@code before}, in every lava-jato (housekeeping job). */
  int forgetReceivedBefore(Instant before);
}
