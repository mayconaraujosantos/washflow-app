package com.washflow.presentation.controllers.user;

import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/auth/washers/pin}: phone + PIN; returns the washer, turned into a session by the
 * adapter.
 */
public class WasherPinLoginController implements Controller {

  private final WasherBadge washerBadge;

  public WasherPinLoginController(WasherBadge washerBadge) {
    this.washerBadge = washerBadge;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          washerBadge.signInWithPin(BodyValues.text(body, "phone"), BodyValues.text(body, "pin")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
