package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.usecases.serviceorder.PauseServiceOrder;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * Parses the HTTP request for pausing a wash and delegates to {@code PauseServiceOrder}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class PauseServiceOrderController implements Controller {

  private final PauseServiceOrder pauseServiceOrder;

  public PauseServiceOrderController(PauseServiceOrder pauseServiceOrder) {
    this.pauseServiceOrder = pauseServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body();
      String reason = BodyValues.text(body, "reason");
      String description = BodyValues.text(body, "description");

      UUID washerId = BodyValues.createdBy(body);
      if (washerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("washerId is required"));
      }

      PauseServiceOrder.Params params =
          new PauseServiceOrder.Params(serviceOrderId, washerId, reason, description);

      pauseServiceOrder.pause(params);
      return new HttpResponse(200, Map.of("status", "PAUSED"));

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
