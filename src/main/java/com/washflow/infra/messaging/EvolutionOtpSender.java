package com.washflow.infra.messaging;

import com.washflow.application.config.EvolutionConfig;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import com.washflow.domain.entities.whatsapp.WhatsappNumber;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The customer's access code by WhatsApp, sent from the platform's own number (the instance in
 * EVOLUTION_INSTANCE) through the Evolution API. The same call works whether the instance is linked
 * by QR code (Baileys) or by the official Cloud API.
 */
public class EvolutionOtpSender implements OtpSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvolutionOtpSender.class);
  static final String FAILURE =
      "Não foi possível enviar o código pelo WhatsApp agora. Tente de novo.";

  private final WhatsappGateway gateway;
  private final String instance;

  public EvolutionOtpSender(EvolutionConfig evolution) {
    this(new EvolutionWhatsappGateway(evolution), evolution.instance());
  }

  public EvolutionOtpSender(WhatsappGateway gateway, String instance) {
    this.gateway = gateway;
    this.instance = instance;
  }

  @Override
  public void send(String phone, String code, String magicLink) {
    try {
      gateway.sendText(instance, WhatsappNumber.of(phone), text(code, magicLink));
    } catch (WhatsappGatewayError e) {
      // The provider's reason goes to the log, never to the customer.
      LOGGER.error("event=otp_whatsapp_failed reason={}", e.getMessage(), e);
      throw new OtpUnavailableError(FAILURE);
    }
  }

  @Override
  public boolean available() {
    return true;
  }

  static String text(String code, String magicLink) {
    return "*"
        + code
        + "* é o seu código de acesso ao Washflow.\n\nOu toque no link para entrar direto:\n"
        + magicLink
        + "\n\nO código vale por 10 minutos. Se não foi você quem pediu, ignore esta mensagem.";
  }
}
