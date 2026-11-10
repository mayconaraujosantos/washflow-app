package com.washflow.presentation.controllers.user;

import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

/** {@code POST /api/users/{id}/badge}: issues the washer's badge (PIN and QR token), shown once. */
public class IssueWasherBadgeController implements Controller {

  private final WasherBadge washerBadge;

  public IssueWasherBadgeController(WasherBadge washerBadge) {
    this.washerBadge = washerBadge;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(washerBadge.issue(UUID.fromString(request.pathParams().get("id"))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
