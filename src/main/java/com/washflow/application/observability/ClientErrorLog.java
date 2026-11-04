package com.washflow.application.observability;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Browser errors reported by the PWA (uncaught exceptions, failed renders). Public endpoint, so it
 * is rate limited per IP and every field is truncated before reaching the log.
 */
public final class ClientErrorLog {

  private static final Logger LOGGER = LoggerFactory.getLogger("client");
  static final int MAX_PER_MINUTE = 20;
  private static final int MAX_MESSAGE = 300;
  private static final int MAX_STACK = 2000;
  private static final int MAX_FIELD = 200;

  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public ClientErrorLog(Clock clock) {
    this.clock = clock;
  }

  /** Returns false when the caller exceeded the rate limit (nothing is logged). */
  public boolean record(String ip, Map<String, Object> body) {
    long minute = Instant.now(clock).getEpochSecond() / 60;
    Window window =
        windows.compute(
            ip == null ? "-" : ip,
            (key, current) ->
                current == null || current.minute() != minute
                    ? new Window(minute, 1)
                    : new Window(minute, current.count() + 1));
    if (window.count() > MAX_PER_MINUTE) {
      return false;
    }
    if (windows.size() > 10_000) {
      windows.entrySet().removeIf(entry -> entry.getValue().minute() != minute);
    }
    LOGGER.warn(
        "Browser error message=\"{}\" page={} source={} stack=\"{}\" agent=\"{}\"",
        field(body, "message", MAX_MESSAGE),
        field(body, "page", MAX_FIELD),
        field(body, "source", MAX_FIELD),
        field(body, "stack", MAX_STACK),
        field(body, "userAgent", MAX_FIELD));
    return true;
  }

  private static String field(Map<String, Object> body, String key, int max) {
    Object value = body == null ? null : body.get(key);
    if (value == null) {
      return "-";
    }
    String text = value.toString().replace('"', '\'').replaceAll("[\\r\\n\\t]+", " | ");
    return text.length() > max ? text.substring(0, max) + "..." : text;
  }

  private record Window(long minute, int count) {}
}
