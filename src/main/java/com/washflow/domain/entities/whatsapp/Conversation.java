package com.washflow.domain.entities.whatsapp;

import java.time.Instant;

/** One phone's chat with the lava-jato's assistant: the pending step and the draft so far. */
public record Conversation(ConversationStep step, ConversationDraft draft, Instant updatedAt) {

  public static Conversation fresh(Instant now) {
    return new Conversation(ConversationStep.MENU, new ConversationDraft(), now);
  }

  public Conversation at(ConversationStep next, Instant now) {
    return new Conversation(next, draft, now);
  }
}
