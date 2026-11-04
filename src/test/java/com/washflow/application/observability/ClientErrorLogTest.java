package com.washflow.application.observability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientErrorLogTest {

  @Test
  void rateLimitsEachIpPerMinute() {
    ClientErrorLog log =
        new ClientErrorLog(Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC));
    Map<String, Object> report = Map.of("message", "x is undefined", "page", "/gerente");

    for (int i = 0; i < ClientErrorLog.MAX_PER_MINUTE; i++) {
      assertTrue(log.record("1.1.1.1", report));
    }
    assertFalse(log.record("1.1.1.1", report));
    assertTrue(log.record("2.2.2.2", report), "other clients are not affected");
  }

  @Test
  void acceptsMalformedOrHugeReportsWithoutFailing() {
    ClientErrorLog log = new ClientErrorLog(Clock.systemUTC());

    assertTrue(log.record(null, null));
    assertTrue(
        log.record("3.3.3.3", Map.of("message", "a".repeat(10_000), "stack", "line1\nline2")));
  }
}
