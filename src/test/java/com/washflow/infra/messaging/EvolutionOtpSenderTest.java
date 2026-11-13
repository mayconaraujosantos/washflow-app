package com.washflow.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.washflow.application.config.EvolutionConfig;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EvolutionOtpSenderTest {

  private HttpServer server;

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsTheCodeAndTheLinkToTheInstanceWithTheApiKey() throws Exception {
    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> apiKey = new AtomicReference<>();
    AtomicReference<String> body = new AtomicReference<>();
    int port =
        start(
            201,
            exchange -> {
              path.set(exchange.getRequestURI().getRawPath());
              apiKey.set(exchange.getRequestHeaders().getFirst("apikey"));
              body.set(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            });

    sender("http://127.0.0.1:" + port + "/", "washflow dev")
        .send("92991234567", "123456", "https://app.test/entrar/whatsapp?codigo=123456");

    assertEquals("/message/sendText/washflow%20dev", path.get());
    assertEquals("secret-key", apiKey.get());
    JsonNode json = new ObjectMapper().readTree(body.get());
    assertEquals("5592991234567", json.path("number").asText());
    assertTrue(json.path("text").asText().contains("123456"));
    assertTrue(
        json.path("text").asText().contains("https://app.test/entrar/whatsapp?codigo=123456"));
  }

  @Test
  void aRefusedSendIsAFriendlyUnavailableError() throws Exception {
    int port = start(400, exchange -> {});

    OtpUnavailableError error =
        assertThrows(
            OtpUnavailableError.class,
            () -> sender("http://127.0.0.1:" + port, "washflow").send("92991234567", "1", "l"));
    assertEquals(EvolutionOtpSender.FAILURE, error.getMessage());
  }

  @Test
  void anUnreachableServerIsAFriendlyUnavailableError() throws Exception {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    EvolutionOtpSender sender = sender("http://127.0.0.1:" + closedPort, "washflow");

    OtpUnavailableError error =
        assertThrows(OtpUnavailableError.class, () -> sender.send("92991234567", "1", "l"));
    assertEquals(EvolutionOtpSender.FAILURE, error.getMessage());
  }

  @Test
  void addsTheBrazilCountryCodeOnlyWhenMissing() {
    assertEquals("559233334444", EvolutionOtpSender.number("9233334444"));
    assertEquals("5592991234567", EvolutionOtpSender.number("92991234567"));
    assertEquals("5592991234567", EvolutionOtpSender.number("+55 (92) 99123-4567"));
  }

  private static EvolutionOtpSender sender(String url, String instance) {
    return new EvolutionOtpSender(new EvolutionConfig(url, "secret-key", instance));
  }

  private interface Inspector {
    void inspect(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
  }

  private int start(int status, Inspector inspector) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          inspector.inspect(exchange);
          byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    return server.getAddress().getPort();
  }
}
