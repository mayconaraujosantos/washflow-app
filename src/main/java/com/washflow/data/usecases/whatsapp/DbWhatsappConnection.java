package com.washflow.data.usecases.whatsapp;

import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.whatsapp.WhatsappInstance;
import com.washflow.domain.errors.whatsapp.WhatsappUnavailableError;
import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Links the current lava-jato's number on its own Evolution instance ({@link WhatsappInstance}).
 * {@code gateway} is null when the server has no WhatsApp provider configured.
 */
public class DbWhatsappConnection implements WhatsappConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbWhatsappConnection.class);
  private static final String PROVIDER_DOWN =
      "Não foi possível falar com o WhatsApp agora. Tente de novo em instantes.";

  private final WhatsappGateway gateway;
  private final WhatsappGateway.Webhook webhook;

  public DbWhatsappConnection(WhatsappGateway gateway, WhatsappGateway.Webhook webhook) {
    this.gateway = gateway;
    this.webhook = webhook;
  }

  @Override
  public ConnectionStatus status() {
    WhatsappGateway.State state = call(() -> gateway().state(instance()));
    return new ConnectionStatus(stateName(state), null);
  }

  @Override
  public ConnectionStatus connect() {
    Optional<String> qrCode = call(() -> gateway().connect(instance(), webhook));
    return qrCode
        .map(code -> new ConnectionStatus(ConnectionStatus.WAITING_QR_CODE, code))
        .orElseGet(() -> new ConnectionStatus(ConnectionStatus.CONNECTED, null));
  }

  @Override
  public void disconnect() {
    call(
        () -> {
          gateway().disconnect(instance());
          return null;
        });
  }

  private WhatsappGateway gateway() {
    if (gateway == null) {
      throw new WhatsappUnavailableError();
    }
    return gateway;
  }

  private static String instance() {
    return WhatsappInstance.nameOf(TenantContext.require());
  }

  private static String stateName(WhatsappGateway.State state) {
    return switch (state) {
      case OPEN -> ConnectionStatus.CONNECTED;
      case CONNECTING -> ConnectionStatus.WAITING_QR_CODE;
      case CLOSED, MISSING -> ConnectionStatus.DISCONNECTED;
    };
  }

  private static <T> T call(Supplier<T> work) {
    try {
      return work.get();
    } catch (WhatsappGatewayError e) {
      LOGGER.error("event=whatsapp_provider_failed reason={}", e.getMessage(), e);
      throw new WhatsappUnavailableError(PROVIDER_DOWN);
    }
  }
}
