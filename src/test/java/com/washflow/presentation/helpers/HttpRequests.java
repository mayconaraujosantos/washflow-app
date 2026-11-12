package com.washflow.presentation.helpers;

import com.washflow.presentation.protocols.HttpRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link HttpRequest}s for controller tests from alternating key/value pairs: {@code
 * body("plate", "ABC1D23", "servicePriceId", 1)}. Values may be {@code null}, to exercise missing
 * fields.
 */
public final class HttpRequests {

  private HttpRequests() {}

  /** A request with only a JSON body. */
  public static HttpRequest body(Object... pairs) {
    return new HttpRequest(pairs(pairs), Map.of(), Map.of());
  }

  /** A request with only query parameters. */
  public static HttpRequest query(String... pairs) {
    return new HttpRequest(Map.of(), Map.of(), pairs((Object[]) pairs));
  }

  /** A request with path parameters and a JSON body. */
  public static HttpRequest request(Map<String, String> path, Object... bodyPairs) {
    return new HttpRequest(pairs(bodyPairs), path, Map.of());
  }

  @SuppressWarnings("unchecked")
  private static <V> Map<String, V> pairs(Object... pairs) {
    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Expected key/value pairs, got " + pairs.length + " items");
    }
    Map<String, V> map = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], (V) pairs[i + 1]);
    }
    return map;
  }
}
