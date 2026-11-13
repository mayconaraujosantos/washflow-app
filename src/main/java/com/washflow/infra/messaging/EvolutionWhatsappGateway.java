package com.washflow.infra.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.application.config.EvolutionConfig;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.protocols.messaging.WhatsappGatewayError;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evolution API v2 over HTTP, authenticated with the server's global {@code apikey}: one instance
 * per number, linked by QR code (Baileys). The webhook only asks for received messages and carries
 * the app's token in the {@link #TOKEN_HEADER} header.
 */
public class EvolutionWhatsappGateway implements WhatsappGateway {

  public static final String TOKEN_HEADER = "X-Washflow-Token";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int NOT_FOUND = 404;

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient http;

  public EvolutionWhatsappGateway(EvolutionConfig evolution) {
    this.baseUrl = evolution.url().replaceAll("/+$", "");
    this.apiKey = evolution.apiKey();
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  }

  @Override
  public State state(String instance) {
    Response response = request("GET", "/instance/connectionState/" + path(instance), null);
    if (response.status() == NOT_FOUND) {
      return State.MISSING;
    }
    requireSuccess(response, "connectionState");
    return switch (response.json().path("instance").path("state").asText("")) {
      case "open" -> State.OPEN;
      case "connecting" -> State.CONNECTING;
      default -> State.CLOSED;
    };
  }

  @Override
  public Optional<String> connect(String instance, Webhook webhook) {
    State state = state(instance);
    if (state == State.MISSING) {
      requireSuccess(
          request(
              "POST",
              "/instance/create",
              Map.of(
                  "instanceName",
                  instance,
                  "integration",
                  "WHATSAPP-BAILEYS",
                  "qrcode",
                  true,
                  "webhook",
                  webhookBody(webhook))),
          "create");
    } else {
      requireSuccess(
          request(
              "POST", "/webhook/set/" + path(instance), Map.of("webhook", webhookBody(webhook))),
          "webhook/set");
    }
    if (state == State.OPEN) {
      return Optional.empty();
    }
    Response qr = request("GET", "/instance/connect/" + path(instance), null);
    requireSuccess(qr, "connect");
    String base64 = qr.json().path("base64").asText("");
    return base64.isBlank() ? Optional.empty() : Optional.of(base64);
  }

  @Override
  public void disconnect(String instance) {
    Response response = request("DELETE", "/instance/logout/" + path(instance), null);
    if (response.status() != NOT_FOUND) {
      requireSuccess(response, "logout");
    }
  }

  @Override
  public void sendText(String instance, String whatsappNumber, String text) {
    requireSuccess(
        request(
            "POST",
            "/message/sendText/" + path(instance),
            Map.of("number", whatsappNumber, "text", text)),
        "sendText");
  }

  private static Map<String, Object> webhookBody(Webhook webhook) {
    return Map.of(
        "enabled",
        true,
        "url",
        webhook.url(),
        "byEvents",
        false,
        "base64",
        false,
        "headers",
        Map.of(TOKEN_HEADER, webhook.token()),
        "events",
        List.of("MESSAGES_UPSERT"));
  }

  private static String path(String segment) {
    return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private Response request(String method, String path, Object body) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(TIMEOUT)
              .header("apikey", apiKey);
      if (body == null) {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      } else {
        builder
            .header("Content-Type", "application/json")
            .method(
                method,
                HttpRequest.BodyPublishers.ofString(
                    JSON.writeValueAsString(body), StandardCharsets.UTF_8));
      }
      HttpResponse<String> response =
          http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WhatsappGatewayError("Interrupted calling the Evolution API", e);
    } catch (IOException | IllegalArgumentException e) {
      throw new WhatsappGatewayError("Evolution API unreachable: " + e.getMessage(), e);
    }
  }

  private static void requireSuccess(Response response, String operation) {
    if (response.status() / 100 != 2) {
      throw new WhatsappGatewayError(
          "Evolution "
              + operation
              + " answered "
              + response.status()
              + ": "
              + abbreviate(response.body()));
    }
  }

  private static String abbreviate(String body) {
    if (body == null) {
      return "";
    }
    return body.length() > 300 ? body.substring(0, 300) + "..." : body;
  }

  private record Response(int status, String body) {

    JsonNode json() {
      try {
        return JSON.readTree(body == null || body.isBlank() ? "{}" : body);
      } catch (IOException e) {
        throw new WhatsappGatewayError("Evolution answered invalid JSON", e);
      }
    }
  }
}
