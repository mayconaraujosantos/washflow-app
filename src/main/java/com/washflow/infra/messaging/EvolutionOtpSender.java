package com.washflow.infra.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.application.AppConfig;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The customer's access code by WhatsApp through a self-hosted Evolution API: {@code POST
 * {EVOLUTION_URL}/message/sendText/{EVOLUTION_INSTANCE}} with the global {@code apikey}. The same
 * call works whether the instance is linked by QR code (Baileys) or by the official Cloud API.
 */
public class EvolutionOtpSender implements OtpSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvolutionOtpSender.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  static final String FAILURE =
      "Não foi possível enviar o código pelo WhatsApp agora. Tente de novo.";

  private final URI endpoint;
  private final String apiKey;
  private final HttpClient http;

  public EvolutionOtpSender(AppConfig.Evolution evolution) {
    String base = evolution.url().replaceAll("/+$", "");
    this.endpoint =
        URI.create(
            base
                + "/message/sendText/"
                + URLEncoder.encode(evolution.instance(), StandardCharsets.UTF_8)
                    .replace("+", "%20"));
    this.apiKey = evolution.apiKey();
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  }

  @Override
  public void send(String phone, String code, String magicLink) {
    try {
      String body =
          JSON.writeValueAsString(Map.of("number", number(phone), "text", text(code, magicLink)));
      HttpRequest request =
          HttpRequest.newBuilder(endpoint)
              .timeout(TIMEOUT)
              .header("Content-Type", "application/json")
              .header("apikey", apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        // The provider's reason goes to the log, never to the customer.
        LOGGER.error(
            "event=otp_whatsapp_failed status={} body={}",
            response.statusCode(),
            abbreviate(response.body()));
        throw new OtpUnavailableError(FAILURE);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OtpUnavailableError(FAILURE);
    } catch (OtpUnavailableError e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error("event=otp_whatsapp_failed reason={}", e.getMessage(), e);
      throw new OtpUnavailableError(FAILURE);
    }
  }

  @Override
  public boolean available() {
    return true;
  }

  /** Phones are stored as DDD + number; WhatsApp wants the country code too. */
  static String number(String phone) {
    String digits = phone.replaceAll("\\D", "");
    return digits.length() <= 11 ? "55" + digits : digits;
  }

  static String text(String code, String magicLink) {
    return "*"
        + code
        + "* é o seu código de acesso ao Washflow.\n\nOu toque no link para entrar direto:\n"
        + magicLink
        + "\n\nO código vale por 10 minutos. Se não foi você quem pediu, ignore esta mensagem.";
  }

  private static String abbreviate(String body) {
    if (body == null) {
      return "";
    }
    return body.length() > 300 ? body.substring(0, 300) + "..." : body;
  }
}
