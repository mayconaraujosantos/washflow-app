package com.washflow.infra.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GoogleTokeninfoVerifierTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");

  private static ObjectNode claims() {
    ObjectNode node = new ObjectMapper().createObjectNode();
    node.put("aud", "client-id");
    node.put("iss", "https://accounts.google.com");
    node.put("exp", NOW.plusSeconds(600).getEpochSecond());
    node.put("email_verified", "true");
    node.put("sub", "123");
    node.put("email", "ana@gmail.com");
    node.put("name", "Ana");
    return node;
  }

  @Test
  void acceptsAValidTokenForOurClient() {
    var identity = GoogleTokeninfoVerifier.parse(claims(), "client-id", NOW).orElseThrow();

    assertEquals("123", identity.subject());
    assertEquals("ana@gmail.com", identity.email());
  }

  @Test
  void rejectsOtherAudiencesIssuersExpiredOrUnverifiedEmails() {
    assertTrue(
        GoogleTokeninfoVerifier.parse(claims().put("aud", "other"), "client-id", NOW).isEmpty());
    assertTrue(
        GoogleTokeninfoVerifier.parse(claims().put("iss", "evil.com"), "client-id", NOW).isEmpty());
    assertTrue(
        GoogleTokeninfoVerifier.parse(
                claims().put("exp", NOW.minusSeconds(1).getEpochSecond()), "client-id", NOW)
            .isEmpty());
    assertTrue(
        GoogleTokeninfoVerifier.parse(claims().put("email_verified", "false"), "client-id", NOW)
            .isEmpty());
  }

  @Test
  void isDisabledWithoutClientId() {
    GoogleTokeninfoVerifier verifier = new GoogleTokeninfoVerifier(" ");

    assertEquals(null, verifier.clientId());
    assertTrue(verifier.verify("anything").isEmpty());
  }
}
