package com.washflow.presentation.controllers.access;

import com.washflow.domain.errors.AccountLockedError;
import com.washflow.domain.errors.GoogleLoginUnavailableError;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.InvalidOtpError;
import com.washflow.domain.errors.OtpTooSoonError;
import com.washflow.domain.errors.OtpUnavailableError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.TicketNotFoundError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.usecases.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.ServiceOrderTicket;
import com.washflow.domain.usecases.WasherBadge;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * Controllers for badge/PIN, ticket and passwordless customer access. Login controllers return the
 * {@code User}; the authenticated route adapter turns it into a signed session.
 */
public final class AccessControllers {

  private static final String ERROR = "error";

  private AccessControllers() {}

  public static Controller issueBadge(WasherBadge washerBadge) {
    return request -> {
      try {
        return HttpHelper.ok(washerBadge.issue(UUID.fromString(request.pathParams().get("id"))));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (UserNotFoundError e) {
        return HttpHelper.notFound(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller pinLogin(WasherBadge washerBadge) {
    return request -> {
      try {
        return HttpHelper.ok(
            washerBadge.signInWithPin(text(request, "phone"), text(request, "pin")));
      } catch (InvalidCredentialsError e) {
        return HttpHelper.unauthorized(e);
      } catch (AccountLockedError e) {
        return new HttpResponse(429, Map.of(ERROR, e.getMessage()));
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller badgeLogin(WasherBadge washerBadge) {
    return request -> {
      try {
        return HttpHelper.ok(washerBadge.signInWithBadge(text(request, "badgeToken")));
      } catch (InvalidCredentialsError e) {
        return HttpHelper.unauthorized(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller issueTicket(ServiceOrderTicket ticket) {
    return request -> {
      try {
        return HttpHelper.ok(
            Map.of("token", ticket.issue(UUID.fromString(request.pathParams().get("id")))));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (ServiceOrderNotFoundError e) {
        return HttpHelper.notFound(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller viewTicket(ServiceOrderTicket ticket) {
    return request -> {
      try {
        return HttpHelper.ok(ticket.view(request.pathParams().get("token")));
      } catch (TicketNotFoundError e) {
        return HttpHelper.notFound(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller options(CustomerPasswordlessLogin login) {
    return request -> {
      try {
        return HttpHelper.ok(login.options());
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller requestCode(CustomerPasswordlessLogin login) {
    return request -> {
      try {
        return HttpHelper.ok(Map.of("expiresInSeconds", login.requestCode(text(request, "phone"))));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (OtpTooSoonError e) {
        return new HttpResponse(429, Map.of(ERROR, e.getMessage()));
      } catch (OtpUnavailableError e) {
        return new HttpResponse(503, Map.of(ERROR, e.getMessage()));
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller verifyCode(CustomerPasswordlessLogin login) {
    return request -> {
      try {
        return HttpHelper.ok(
            login.verifyCode(text(request, "phone"), text(request, "code"), text(request, "name")));
      } catch (IllegalArgumentException e) {
        // Valid code for a new phone: the app asks for the customer's name.
        return new HttpResponse(400, Map.of(ERROR, e.getMessage(), "needsName", true));
      } catch (InvalidOtpError e) {
        return HttpHelper.unauthorized(e);
      } catch (UserProfileMismatchError e) {
        return HttpHelper.conflict(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller google(CustomerPasswordlessLogin login) {
    return request -> {
      try {
        return HttpHelper.ok(login.signInWithGoogle(text(request, "credential")));
      } catch (GoogleLoginUnavailableError e) {
        return new HttpResponse(503, Map.of(ERROR, e.getMessage()));
      } catch (InvalidCredentialsError e) {
        return HttpHelper.unauthorized(e);
      } catch (UserProfileMismatchError e) {
        return HttpHelper.conflict(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  private static String text(HttpRequest request, String key) {
    Object value = request.body() == null ? null : request.body().get(key);
    return value == null ? null : value.toString();
  }
}
