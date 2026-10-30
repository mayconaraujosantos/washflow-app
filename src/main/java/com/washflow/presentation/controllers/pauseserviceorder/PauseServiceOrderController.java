package com.washflow.presentation.controllers.pauseserviceorder;

import com.washflow.domain.errors.ServiceOrderNotPausedError;
import com.washflow.domain.usecases.PauseServiceOrder;
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

      @SuppressWarnings("unchecked")
      Map<String, Object> body = request.body();
      String reason = (String) body.get("reason");
      String description = (String) body.get("description");

      UUID washerId = (UUID) body.get("createdBy");
      if (washerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("washerId is required"));
      }

      PauseServiceOrder.Params params =
          new PauseServiceOrder.Params(serviceOrderId, washerId, reason, description);

      pauseServiceOrder.pause(params);
      return new HttpResponse(200, Map.of("status", "PAUSED"));

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotPausedError e) {
      return new HttpResponse(409, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
