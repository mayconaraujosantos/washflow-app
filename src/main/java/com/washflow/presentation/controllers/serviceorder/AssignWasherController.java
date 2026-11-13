package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.usecases.serviceorder.AssignWasher;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

/**
 * {@code PATCH /api/service-orders/{id}/washer} (manager): {@code {"washerId": "..."}} reserves the
 * car for that washer, {@code {"washerId": null}} gives it back to the shared queue.
 */
public class AssignWasherController implements Controller {

  private final AssignWasher assignWasher;

  public AssignWasherController(AssignWasher assignWasher) {
    this.assignWasher = assignWasher;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      UUID washerId = BodyValues.uuid(request.body(), "washerId");
      return HttpHelper.ok(assignWasher.assign(new AssignWasher.Params(serviceOrderId, washerId)));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
