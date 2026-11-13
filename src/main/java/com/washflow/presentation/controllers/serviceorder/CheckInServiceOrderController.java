package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.usecases.serviceorder.CheckInServiceOrder;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class CheckInServiceOrderController implements Controller {

  private final CheckInServiceOrder checkInServiceOrder;

  public CheckInServiceOrderController(CheckInServiceOrder checkInServiceOrder) {
    this.checkInServiceOrder = checkInServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      // Optional: the washer the manager picked; absent sends the car to the shared queue.
      UUID washerId = BodyValues.uuid(request.body(), "washerId");

      var serviceOrder =
          checkInServiceOrder.checkIn(new CheckInServiceOrder.Params(serviceOrderId, washerId));

      return HttpHelper.ok(serviceOrder);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
