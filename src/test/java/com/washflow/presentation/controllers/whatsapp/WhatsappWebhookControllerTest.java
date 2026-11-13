package com.washflow.presentation.controllers.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.domain.usecases.whatsapp.ReceiveWhatsappMessage;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WhatsappWebhookControllerTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(String json) throws Exception {
    return JSON.readValue(json, Map.class);
  }

  @Test
  void readsATextMessage() throws Exception {
    ReceiveWhatsappMessage receive = mock(ReceiveWhatsappMessage.class);

    int status =
        new WhatsappWebhookController(receive)
            .handle(
                new HttpRequest(
                    body(
                        """
                        {"event":"messages.upsert","instance":"washflow-lj",
                         "data":{"key":{"remoteJid":"5592991234567@s.whatsapp.net",
                                        "fromMe":false,"id":"ABC"},
                                 "pushName":"Ana","message":{"conversation":"oi"}}}
                        """),
                    Map.of(),
                    Map.of()))
            .statusCode();

    assertEquals(200, status);
    verify(receive)
        .receive(new ReceiveWhatsappMessage.Params("washflow-lj", "ABC", "5592991234567", "oi"));
  }

  @Test
  void readsRepliesAndPrivateIdChats() throws Exception {
    Optional<ReceiveWhatsappMessage.Params> params =
        WhatsappWebhookController.parse(
            body(
                """
                {"event":"MESSAGES_UPSERT","instance":"washflow-lj",
                 "data":{"key":{"remoteJid":"1234567890@lid",
                                "remoteJidAlt":"5592991234567@s.whatsapp.net","id":"X"},
                         "message":{"extendedTextMessage":{"text":"quero lavar"}}}}
                """));

    assertEquals(
        Optional.of(
            new ReceiveWhatsappMessage.Params("washflow-lj", "X", "5592991234567", "quero lavar")),
        params);
  }

  @Test
  void anAudioHasNoText() throws Exception {
    Optional<ReceiveWhatsappMessage.Params> params =
        WhatsappWebhookController.parse(
            body(
                """
                {"event":"messages.upsert","instance":"i",
                 "data":{"key":{"remoteJid":"5592991234567@s.whatsapp.net","id":"A"},
                         "message":{"audioMessage":{"seconds":3}}}}
                """));

    assertTrue(params.isPresent());
    assertEquals(null, params.get().text());
  }

  @Test
  void ignoresOwnMessagesGroupsAndOtherEvents() throws Exception {
    ReceiveWhatsappMessage receive = mock(ReceiveWhatsappMessage.class);
    WhatsappWebhookController controller = new WhatsappWebhookController(receive);

    for (String json :
        new String[] {
          """
          {"event":"messages.upsert","data":{"key":{"remoteJid":"5592991234567@s.whatsapp.net",
           "fromMe":true,"id":"1"},"message":{"conversation":"Agendado"}}}
          """,
          """
          {"event":"messages.upsert","data":{"key":{"remoteJid":"120363@g.us","id":"2"},
           "message":{"conversation":"oi"}}}
          """,
          """
          {"event":"connection.update","data":{"state":"open"}}
          """
        }) {
      assertEquals(
          200, controller.handle(new HttpRequest(body(json), Map.of(), Map.of())).statusCode());
    }

    verifyNoInteractions(receive);
  }
}
