package com.washflow.presentation.helpers;

import com.washflow.presentation.errors.ServerError;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Analogous to clean-ts-api's http-helper: one place that builds every standard HttpResponse. */
public final class HttpHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);

  private HttpHelper() {}

  public static HttpResponse ok(Object body) {
    return new HttpResponse(200, body);
  }

  public static HttpResponse badRequest(Exception error) {
    return new HttpResponse(400, Map.of("error", error.getMessage()));
  }

  public static HttpResponse notFound(Exception error) {
    return new HttpResponse(404, Map.of("error", error.getMessage()));
  }

  public static HttpResponse serverError(Exception error) {
    LOGGER.error("Unhandled controller error", new ServerError(error));
    return new HttpResponse(500, Map.of("error", "Internal server error"));
  }
}
