package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.usecases.serviceorder.CancelServiceOrder;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/** Parses the manager's cancellation request and delegates to {@code CancelServiceOrder}. */
public class CancelServiceOrderController implements Controller {

  private final CancelServiceOrder cancelServiceOrder;

  public CancelServiceOrderController(CancelServiceOrder cancelServiceOrder) {
    this.cancelServiceOrder = cancelServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body();
      UUID managerId = BodyValues.createdBy(body);
      if (managerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("managerId is required"));
      }
      cancelServiceOrder.cancel(
          new CancelServiceOrder.Params(
              serviceOrderId,
              managerId,
              BodyValues.text(body, "reason"),
              BodyValues.text(body, "description")));
      return new HttpResponse(200, Map.of("status", "CANCELLED"));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
