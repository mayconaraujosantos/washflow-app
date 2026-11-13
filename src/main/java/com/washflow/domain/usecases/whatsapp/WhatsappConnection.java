package com.washflow.domain.usecases.whatsapp;

/**
 * The manager links the lava-jato's own WhatsApp number by scanning a QR code; from then on the
 * customers get status notices and can schedule by chatting with that number. Implemented by {@code
 * DbWhatsappConnection} in the data layer.
 */
public interface WhatsappConnection {

  ConnectionStatus status();

  /** Starts linking: the QR code to scan, or the current status when already linked. */
  ConnectionStatus connect();

  void disconnect();

  /**
   * {@code state}: "CONNECTED", "WAITING_QR_CODE" or "DISCONNECTED". {@code qrCode} is a {@code
   * data:image/png;base64,...} URL while waiting, otherwise null.
   */
  record ConnectionStatus(String state, String qrCode) {

    public static final String CONNECTED = "CONNECTED";
    public static final String WAITING_QR_CODE = "WAITING_QR_CODE";
    public static final String DISCONNECTED = "DISCONNECTED";
  }
}
