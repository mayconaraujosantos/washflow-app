package com.washflow.presentation.controllers.user;

import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/auth/washers/badge}: the badge QR token; returns the washer, turned into a
 * session by the adapter.
 */
public class WasherBadgeLoginController implements Controller {

  private final WasherBadge washerBadge;

  public WasherBadgeLoginController(WasherBadge washerBadge) {
    this.washerBadge = washerBadge;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(washerBadge.signInWithBadge(BodyValues.text(body, "badgeToken")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
