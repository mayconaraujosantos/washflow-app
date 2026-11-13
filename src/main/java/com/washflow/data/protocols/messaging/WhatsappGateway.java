package com.washflow.data.protocols.messaging;

import java.util.Optional;

/**
 * The WhatsApp provider (Evolution API): one instance per lava-jato number, linked by QR code.
 * Every call throws {@link WhatsappGatewayError} when the provider can't be reached or refuses.
 */
public interface WhatsappGateway {

  State state(String instance);

  /**
   * Creates the instance if needed, points its webhook at the app and returns the QR code to scan
   * (a {@code data:image/png;base64,...} URL); empty when the number is already linked.
   */
  Optional<String> connect(String instance, Webhook webhook);

  /** Unlinks the number; the instance stays, ready for another QR code. */
  void disconnect(String instance);

  void sendText(String instance, String whatsappNumber, String text);

  enum State {
    /** Linked and online. */
    OPEN,
    /** Waiting for the QR code to be scanned. */
    CONNECTING,
    /** Created but not linked (or logged out). */
    CLOSED,
    /** Never created on the provider. */
    MISSING
  }

  /** Where the provider posts received messages, and the token it sends back in a header. */
  record Webhook(String url, String token) {

    @Override
    public String toString() {
      return "Webhook[url=" + url + ", token=[redacted]]";
    }
  }
}
