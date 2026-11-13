package com.washflow.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.washflow.application.config.EvolutionConfig;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvolutionWhatsappGatewayTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private HttpServer server;
  private final Map<String, Reply> replies = new HashMap<>();
  private final List<String> calls = new ArrayList<>();
  private final Map<String, String> bodies = new HashMap<>();
  private EvolutionWhatsappGateway gateway;

  private record Reply(int status, String body) {}

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String call = exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath();
          calls.add(call);
          bodies.put(
              call, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          if (!"global-key".equals(exchange.getRequestHeaders().getFirst("apikey"))) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
          }
          Reply reply = replies.getOrDefault(call, new Reply(200, "{}"));
          byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(reply.status(), bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    gateway =
        new EvolutionWhatsappGateway(
            new EvolutionConfig(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/", "global-key", ""));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void mapsTheConnectionState() {
    replies.put(
        "GET /instance/connectionState/lj-1",
        new Reply(200, "{\"instance\":{\"instanceName\":\"lj-1\",\"state\":\"open\"}}"));
    replies.put("GET /instance/connectionState/lj-2", new Reply(404, "{\"status\":404}"));
    replies.put(
        "GET /instance/connectionState/lj-3",
        new Reply(200, "{\"instance\":{\"state\":\"close\"}}"));

    assertEquals(WhatsappGateway.State.OPEN, gateway.state("lj-1"));
    assertEquals(WhatsappGateway.State.MISSING, gateway.state("lj-2"));
    assertEquals(WhatsappGateway.State.CLOSED, gateway.state("lj-3"));
  }

  @Test
  void createsAMissingInstanceWithTheWebhookAndReturnsTheQrCode() throws Exception {
    replies.put("GET /instance/connectionState/lj-1", new Reply(404, "{}"));
    replies.put("POST /instance/create", new Reply(201, "{}"));
    replies.put(
        "GET /instance/connect/lj-1",
        new Reply(200, "{\"base64\":\"data:image/png;base64,QR\",\"code\":\"2@x\"}"));

    Optional<String> qr =
        gateway.connect("lj-1", new WhatsappGateway.Webhook("http://api/hook", "token-123"));

    assertEquals(Optional.of("data:image/png;base64,QR"), qr);
    JsonNode created = JSON.readTree(bodies.get("POST /instance/create"));
    assertEquals("lj-1", created.path("instanceName").asText());
    assertEquals("WHATSAPP-BAILEYS", created.path("integration").asText());
    assertEquals("http://api/hook", created.path("webhook").path("url").asText());
    assertEquals(
        "token-123",
        created
            .path("webhook")
            .path("headers")
            .path(EvolutionWhatsappGateway.TOKEN_HEADER)
            .asText());
    assertEquals("MESSAGES_UPSERT", created.path("webhook").path("events").get(0).asText());
  }

  @Test
  void anAlreadyLinkedNumberOnlyRefreshesTheWebhook() {
    replies.put(
        "GET /instance/connectionState/lj-1",
        new Reply(200, "{\"instance\":{\"state\":\"open\"}}"));

    Optional<String> qr =
        gateway.connect("lj-1", new WhatsappGateway.Webhook("http://api/hook", "token-123"));

    assertTrue(qr.isEmpty());
    assertTrue(calls.contains("POST /webhook/set/lj-1"));
    assertTrue(calls.stream().noneMatch(call -> call.startsWith("GET /instance/connect/")));
  }

  @Test
  void sendsTextToTheInstance() throws Exception {
    gateway.sendText("lj-1", "5592991234567", "Olá");

    JsonNode body = JSON.readTree(bodies.get("POST /message/sendText/lj-1"));
    assertEquals("5592991234567", body.path("number").asText());
    assertEquals("Olá", body.path("text").asText());
  }

  @Test
  void aRefusalIsAGatewayError() {
    replies.put("POST /message/sendText/lj-1", new Reply(400, "{\"error\":\"not connected\"}"));

    WhatsappGatewayError error =
        assertThrows(WhatsappGatewayError.class, () -> gateway.sendText("lj-1", "55", "x"));
    assertTrue(error.getMessage().contains("400"));
  }
}
