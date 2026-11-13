package com.washflow.domain.entities.whatsapp;

/** Where a customer's chat with the WhatsApp assistant stands: what the next message answers. */
public enum ConversationStep {
  /** Nothing pending: the next message picks an option of the menu (or is free text). */
  MENU,
  /** First contact from an unknown number: the next message is the customer's name. */
  ASK_NAME,
  CHOOSE_VEHICLE,
  ASK_PLATE,
  ASK_MODEL,
  CHOOSE_SERVICES,
  CHOOSE_DAY,
  CHOOSE_TIME,
  CONFIRM,
  /** "Meus agendamentos": the next message picks a scheduled order to cancel. */
  CHOOSE_ORDER,
  CONFIRM_CANCEL,
  /** Handed over to the lava-jato's staff: the assistant stays quiet until "menu" or timeout. */
  HUMAN
}
