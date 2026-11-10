package com.washflow.presentation.controllers.cancelserviceorder;

import com.washflow.domain.errors.ServiceOrderNotCancellableError;
import com.washflow.domain.usecases.CancelServiceOrder;
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
      UUID managerId = (UUID) body.get("createdBy");
      if (managerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("managerId is required"));
      }
      cancelServiceOrder.cancel(
          new CancelServiceOrder.Params(
              serviceOrderId,
              managerId,
              (String) body.get("reason"),
              (String) body.get("description")));
      return new HttpResponse(200, Map.of("status", "CANCELLED"));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotCancellableError e) {
      return new HttpResponse(409, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
