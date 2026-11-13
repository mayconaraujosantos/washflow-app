package com.washflow.presentation.controllers.whatsapp;

import com.washflow.domain.entities.whatsapp.WhatsappNumber;
import com.washflow.domain.usecases.whatsapp.ReceiveWhatsappMessage;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code POST /api/whatsapp/webhook}: the Evolution API delivers each message received by a
 * lava-jato's number ({@code messages.upsert}). Messages sent by the number itself, groups and
 * anything without a sender phone are acknowledged and ignored. The route checks the webhook token
 * before this controller runs.
 */
public class WhatsappWebhookController implements Controller {

  private static final Map<String, Object> ACCEPTED = Map.of("received", true);

  private final ReceiveWhatsappMessage receiveWhatsappMessage;

  public WhatsappWebhookController(ReceiveWhatsappMessage receiveWhatsappMessage) {
    this.receiveWhatsappMessage = receiveWhatsappMessage;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      parse(request.body()).ifPresent(receiveWhatsappMessage::receive);
      return HttpHelper.ok(ACCEPTED);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  static Optional<ReceiveWhatsappMessage.Params> parse(Map<String, Object> body) {
    String event = text(body, "event").toLowerCase(Locale.ROOT).replace('_', '.');
    Map<String, Object> data = map(body.get("data"));
    Map<String, Object> key = map(data.get("key"));
    if (!"messages.upsert".equals(event) || Boolean.TRUE.equals(key.get("fromMe"))) {
      return Optional.empty();
    }
    Optional<String> sender = sender(key);
    String messageId = text(key, "id");
    if (sender.isEmpty() || messageId.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new ReceiveWhatsappMessage.Params(
            text(body, "instance"),
            messageId,
            sender.get(),
            messageText(map(data.get("message")))));
  }

  /**
   * The phone JID; newer WhatsApp versions may address the chat by a private id ({@code @lid}) and
   * carry the phone in {@code remoteJidAlt} or {@code senderPn}.
   */
  private static Optional<String> sender(Map<String, Object> key) {
    for (String field : new String[] {"remoteJid", "remoteJidAlt", "senderPn"}) {
      Optional<String> phone = WhatsappNumber.fromJid(text(key, field));
      if (phone.isPresent()) {
        return phone;
      }
    }
    return Optional.empty();
  }

  private static String messageText(Map<String, Object> message) {
    String plain = text(message, "conversation");
    if (!plain.isBlank()) {
      return plain;
    }
    String extended = text(map(message.get("extendedTextMessage")), "text");
    return extended.isBlank() ? null : extended;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object value) {
    return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  private static String text(Map<String, Object> map, String field) {
    return map.get(field) instanceof String value ? value : "";
  }
}
