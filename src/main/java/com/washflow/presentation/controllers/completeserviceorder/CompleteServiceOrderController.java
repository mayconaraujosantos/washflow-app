package com.washflow.presentation.controllers.completeserviceorder;

import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.CompleteServiceOrder;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class CompleteServiceOrderController implements Controller {

  private final CompleteServiceOrder completeServiceOrder;

  public CompleteServiceOrderController(CompleteServiceOrder completeServiceOrder) {
    this.completeServiceOrder = completeServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      var serviceOrder =
          completeServiceOrder.complete(new CompleteServiceOrder.Params(serviceOrderId));

      return HttpHelper.ok(serviceOrder);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (InvalidServiceOrderStatusError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
