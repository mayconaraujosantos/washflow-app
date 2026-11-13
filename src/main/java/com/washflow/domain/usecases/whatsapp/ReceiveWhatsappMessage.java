package com.washflow.domain.usecases.whatsapp;

/**
 * A customer wrote to a lava-jato's WhatsApp number: the assistant answers from that number.
 * Implemented by {@code DbReceiveWhatsappMessage} in the data layer.
 */
public interface ReceiveWhatsappMessage {

  void receive(Params params);

  /**
   * {@code instance} names the lava-jato's number, {@code messageId} is the provider's id (a resent
   * webhook carries the same one), {@code whatsappNumber} the sender with country code and {@code
   * text} null for anything that isn't text (audio, photo, sticker).
   */
  record Params(String instance, String messageId, String whatsappNumber, String text) {}
}
