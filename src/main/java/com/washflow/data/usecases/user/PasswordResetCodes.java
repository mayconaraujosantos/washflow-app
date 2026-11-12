package com.washflow.data.usecases.user;

import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.data.protocols.db.user.PasswordResetRepository.PendingCode;
import com.washflow.data.usecases.shared.Secrets;
import com.washflow.domain.errors.auth.InvalidOtpError;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Lifecycle of the e-mailed password-reset code of an account: stored only as a keyed hash, expires
 * after {@link #CODE_TTL}, allows {@link #MAX_ATTEMPTS} guesses and can be re-sent after {@link
 * #RESEND_AFTER}.
 */
public class PasswordResetCodes {

  static final Duration CODE_TTL = Duration.ofMinutes(10);
  static final Duration RESEND_AFTER = Duration.ofSeconds(60);
  static final int MAX_ATTEMPTS = 5;
  private static final int CODE_DIGITS = 6;

  private final PasswordResetRepository repository;
  private final TokenHasher tokenHasher;
  private final Clock clock;

  public PasswordResetCodes(
      PasswordResetRepository repository, TokenHasher tokenHasher, Clock clock) {
    this.repository = repository;
    this.tokenHasher = tokenHasher;
    this.clock = clock;
  }

  /** A new code for the account, or empty when one was sent less than {@link #RESEND_AFTER} ago. */
  Optional<String> issue(UUID userId) {
    Instant now = Instant.now(clock);
    boolean justSent =
        repository
            .loadCode(userId)
            .map(pending -> pending.createdAt().plus(RESEND_AFTER).isAfter(now))
            .orElse(false);
    if (justSent) {
      return Optional.empty();
    }
    String code = Secrets.digits(CODE_DIGITS);
    repository.saveCode(userId, hash(userId, code), now.plus(CODE_TTL), now);
    return Optional.of(code);
  }

  /**
   * Accepts the pending code of the account. A wrong guess counts towards {@link #MAX_ATTEMPTS}; an
   * expired or exhausted code is discarded. The code stays pending until {@link #discard}.
   */
  void verify(UUID userId, String code) {
    PendingCode pending = repository.loadCode(userId).orElseThrow(InvalidOtpError::new);
    if (pending.expiresAt().isBefore(Instant.now(clock)) || pending.attempts() >= MAX_ATTEMPTS) {
      repository.deleteCode(userId);
      throw new InvalidOtpError();
    }
    if (code == null || !sameHash(pending.codeHash(), hash(userId, code.trim()))) {
      repository.incrementAttempts(userId);
      throw new InvalidOtpError();
    }
  }

  void discard(UUID userId) {
    repository.deleteCode(userId);
  }

  int ttlSeconds() {
    return (int) CODE_TTL.toSeconds();
  }

  private String hash(UUID userId, String code) {
    return tokenHasher.hash("password-reset:" + userId + ":" + code);
  }

  /** Constant-time comparison, so response timing does not leak how much of a guess matched. */
  private static boolean sameHash(String expected, String actual) {
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
