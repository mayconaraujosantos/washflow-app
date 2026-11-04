package com.washflow.application.idempotency;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence of {@code Idempotency-Key} requests and their responses ({@link IdempotencyGuard}).
 */
public interface IdempotencyStore {

  /** What a request carrying an already-known key should do. */
  sealed interface Claim {}

  /** First time this key is seen (or a stale attempt was taken over): run the request. */
  record Acquired() implements Claim {}

  /** Another request with this key is still running. */
  record InProgress(String requestHash) implements Claim {}

  /** Already answered successfully: send the same response again. */
  record Replay(String requestHash, int status, String body) implements Claim {}

  Claim claim(
      UUID userId,
      String key,
      UUID establishmentId,
      String method,
      String route,
      String requestHash,
      Instant now);

  /** Stores the successful response for the key. */
  void complete(UUID userId, String key, int status, String body);

  /** Forgets the key (the request failed): a retry with it runs again. */
  void release(UUID userId, String key);
}
