package com.washflow.data.usecases.whatsapp;

import com.washflow.data.protocols.db.whatsapp.NotificationRepository;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.whatsapp.WhatsappInstance;
import com.washflow.domain.entities.whatsapp.WhatsappNumber;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Status notices sent from the lava-jato's own number. A notice never fails the operation that
 * triggered it: a lava-jato without a linked number, a customer without a phone or a provider error
 * only goes to the log. {@link #ready} runs on {@code executor}, so the washer's request doesn't
 * wait for WhatsApp.
 */
public class WhatsappNotices {

  private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappNotices.class);
  private static final DateTimeFormatter HOUR =
      DateTimeFormatter.ofPattern("HH:mm").withZone(SchedulingRules.STORE_ZONE);

  private final NotificationRepository repository;
  private final WhatsappGateway gateway;
  private final Executor executor;

  public WhatsappNotices(
      NotificationRepository repository, WhatsappGateway gateway, Executor executor) {
    this.repository = repository;
    this.gateway = gateway;
    this.executor = executor;
  }

  /** "Seu carro está pronto": after the washer finishes, in the background. */
  public void ready(UUID serviceOrderId) {
    UUID establishmentId = TenantContext.require();
    executor.execute(
        () ->
            TenantContext.runAs(
                establishmentId,
                () -> {
                  send(establishmentId, serviceOrderId, "ready", WhatsappNotices::readyText);
                  return null;
                }));
  }

  /** The reminder an hour before the appointment; runs inside the order's lava-jato. */
  void reminder(UUID establishmentId, UUID serviceOrderId) {
    send(establishmentId, serviceOrderId, "reminder", WhatsappNotices::reminderText);
  }

  private void send(
      UUID establishmentId,
      UUID serviceOrderId,
      String notice,
      java.util.function.Function<NotificationRepository.Recipient, String> text) {
    try {
      repository
          .recipientOf(serviceOrderId)
          .ifPresent(
              recipient ->
                  gateway.sendText(
                      WhatsappInstance.nameOf(establishmentId),
                      WhatsappNumber.of(recipient.phone()),
                      text.apply(recipient)));
    } catch (WhatsappGatewayError e) {
      // Expected while the lava-jato hasn't linked a number: not an error of the app.
      LOGGER.warn(
          "event=whatsapp_notice_failed notice={} service_order={} reason={}",
          notice,
          serviceOrderId,
          e.getMessage());
    } catch (RuntimeException e) {
      LOGGER.error(
          "event=whatsapp_notice_failed notice={} service_order={}", notice, serviceOrderId, e);
    }
  }

  static String readyText(NotificationRepository.Recipient recipient) {
    return greeting(recipient)
        + "Seu "
        + vehicle(recipient)
        + " está pronto para retirada no "
        + recipient.establishmentName()
        + ". 🚗✨";
  }

  static String reminderText(NotificationRepository.Recipient recipient) {
    return greeting(recipient)
        + "Lembrete: a lavagem do seu "
        + vehicle(recipient)
        + " no "
        + recipient.establishmentName()
        + " é hoje às "
        + HOUR.format(recipient.scheduledAt())
        + ".\n\nSe não puder vir, responda *menu* para cancelar.";
  }

  private static String greeting(NotificationRepository.Recipient recipient) {
    String name = recipient.customerName();
    if (name == null || name.isBlank()) {
      return "Olá! ";
    }
    return "Olá, " + name.trim().split("\\s+")[0] + "! ";
  }

  private static String vehicle(NotificationRepository.Recipient recipient) {
    String model = recipient.vehicleModel();
    return (model == null || model.isBlank() ? "carro" : model)
        + " ("
        + recipient.vehiclePlate()
        + ")";
  }
}
