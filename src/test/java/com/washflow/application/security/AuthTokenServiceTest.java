package com.washflow.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Test
  void issuedTokenContainsTheUserIdentityAndRole() {
    AuthTokenService service = new AuthTokenService(SECRET);
    UUID userId = UUID.randomUUID();
    User user = new User(userId, "Ana", "11999990000", UserProfile.WASHER, Instant.now());

    AuthTokenService.Claims claims = service.verify(service.issue(user));

    assertEquals(userId, claims.userId());
    assertEquals(UserProfile.WASHER, claims.profile());
  }

  @Test
  void rejectsTokensSignedWithAnotherSecret() {
    User user =
        new User(UUID.randomUUID(), "Ana", "11999990000", UserProfile.CUSTOMER, Instant.now());
    String token = new AuthTokenService(SECRET).issue(user);

    assertThrows(
        AuthTokenService.InvalidTokenException.class,
        () -> new AuthTokenService("abcdef0123456789abcdef0123456789").verify(token));
  }

  @Test
  void rejectsMalformedTokens() {
    assertThrows(
        AuthTokenService.InvalidTokenException.class,
        () -> new AuthTokenService(SECRET).verify("not-a-token"));
  }
}
