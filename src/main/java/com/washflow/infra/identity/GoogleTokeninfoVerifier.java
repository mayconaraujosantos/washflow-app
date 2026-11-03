package com.washflow.infra.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.data.protocols.identity.GoogleTokenVerifier;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Validates ID tokens with Google's tokeninfo endpoint (Google checks the signature), then checks
 * audience, issuer, expiry and verified e-mail here. {@code clientId} null disables Google login.
 */
public class GoogleTokeninfoVerifier implements GoogleTokenVerifier {

  private static final Set<String> ISSUERS =
      Set.of("accounts.google.com", "https://accounts.google.com");
  private static final ObjectMapper JSON = new ObjectMapper();

  private final String clientId;
  private final HttpClient http;

  public GoogleTokeninfoVerifier(String clientId) {
    this.clientId = clientId == null || clientId.isBlank() ? null : clientId.trim();
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public String clientId() {
    return clientId;
  }

  @Override
  public Optional<GoogleIdentity> verify(String idToken) {
    if (clientId == null || idToken == null || idToken.isBlank()) {
      return Optional.empty();
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(
                      "https://oauth2.googleapis.com/tokeninfo?id_token="
                          + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      return parse(JSON.readTree(response.body()), clientId, Instant.now());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  static Optional<GoogleIdentity> parse(JsonNode claims, String clientId, Instant now) {
    boolean valid =
        clientId.equals(claims.path("aud").asText())
            && ISSUERS.contains(claims.path("iss").asText())
            && claims.path("exp").asLong(0) > now.getEpochSecond()
            && "true".equals(claims.path("email_verified").asText())
            && !claims.path("sub").asText().isBlank();
    if (!valid) {
      return Optional.empty();
    }
    return Optional.of(
        new GoogleIdentity(
            claims.path("sub").asText(),
            claims.path("email").asText(null),
            claims.path("name").asText(null)));
  }
}
