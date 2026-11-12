package com.washflow.presentation.helpers;

import com.washflow.domain.errors.common.AccessDeniedError;
import com.washflow.domain.errors.common.AuthenticationError;
import com.washflow.domain.errors.common.ConflictError;
import com.washflow.domain.errors.common.DomainError;
import com.washflow.domain.errors.common.NotFoundError;
import com.washflow.domain.errors.common.TooManyRequestsError;
import com.washflow.domain.errors.common.UnavailableError;
import com.washflow.domain.errors.common.ValidationError;
import com.washflow.presentation.errors.ServerError;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Analogous to clean-ts-api's http-helper: one place that builds every standard HttpResponse. */
public final class HttpHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);
  private static final String ERROR = "error";

  /** MDC keys shared with the request access log (RequestTracing). */
  public static final String REQUEST_ID = "request_id";

  public static final String ERROR_REASON = "error_reason";

  private HttpHelper() {}

  public static HttpResponse ok(Object body) {
    return new HttpResponse(200, body);
  }

  public static HttpResponse badRequest(Exception error) {
    return new HttpResponse(400, errorBody(error.getMessage()));
  }

  public static HttpResponse notFound(Exception error) {
    return new HttpResponse(404, errorBody(error.getMessage()));
  }

  public static HttpResponse conflict(Exception error) {
    return new HttpResponse(409, errorBody(error.getMessage()));
  }

  public static HttpResponse forbidden(Exception error) {
    return new HttpResponse(403, errorBody(error.getMessage()));
  }

  public static HttpResponse unauthorized(Exception error) {
    return new HttpResponse(401, errorBody(error.getMessage()));
  }

  /**
   * The response for an error a controller did not handle itself: a {@link DomainError} gets the
   * status of its category, anything else is a bug and becomes a logged 500.
   */
  public static HttpResponse serverError(Exception error) {
    if (error instanceof DomainError domainError) {
      return domainError(domainError);
    }
    LOGGER.error("Unhandled controller error", new ServerError(error));
    return new HttpResponse(500, errorBody("Internal server error"));
  }

  /** {@code {"error": message, "code": code, "requestId": id}} with the status of its category. */
  public static HttpResponse domainError(DomainError error) {
    Map<String, Object> body = errorBody(error.getMessage());
    body.put("code", error.code());
    return new HttpResponse(statusOf(error), body);
  }

  static int statusOf(DomainError error) {
    return switch (error) {
      case ValidationError invalid -> 400;
      case AuthenticationError unauthenticated -> 401;
      case AccessDeniedError denied -> 403;
      case NotFoundError notFound -> 404;
      case ConflictError conflict -> 409;
      case TooManyRequestsError tooMany -> 429;
      case UnavailableError unavailable -> 503;
    };
  }

  /**
   * {@code {"error": message, "requestId": id}}. The reason also goes to the MDC so the request's
   * access log line says why it failed, and the id lets a user's report be matched to the logs.
   */
  public static Map<String, Object> errorBody(String message) {
    String reason = message == null ? "" : message;
    MDC.put(ERROR_REASON, reason);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put(ERROR, reason);
    String requestId = MDC.get(REQUEST_ID);
    if (requestId != null) {
      body.put("requestId", requestId);
    }
    return body;
  }
}
