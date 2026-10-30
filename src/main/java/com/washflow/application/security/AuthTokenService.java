package com.washflow.application.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class AuthTokenService {

  private static final long TOKEN_LIFETIME_SECONDS = 8 * 60 * 60;
  private final Algorithm algorithm;

  public AuthTokenService(String secret) {
    algorithm = Algorithm.HMAC256(secret);
  }

  public String issue(User user) {
    Instant now = Instant.now();
    return JWT.create()
        .withSubject(user.id().toString())
        .withClaim("profile", user.profile().dbValue())
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(now.plusSeconds(TOKEN_LIFETIME_SECONDS)))
        .sign(algorithm);
  }

  public Claims verify(String token) {
    try {
      var decoded = JWT.require(algorithm).build().verify(token);
      UUID userId = UUID.fromString(decoded.getSubject());
      UserProfile profile = UserProfile.fromDbValue(decoded.getClaim("profile").asString());
      return new Claims(userId, profile);
    } catch (JWTVerificationException | IllegalArgumentException e) {
      throw new InvalidTokenException();
    }
  }

  public record Claims(UUID userId, UserProfile profile) {}

  public static final class InvalidTokenException extends RuntimeException {}
}
