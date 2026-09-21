package com.washflow.presentation.controllers.finalizeserviceorder;

import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.usecases.FinalizeServiceOrder;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class FinalizeServiceOrderController implements Controller {

  private final FinalizeServiceOrder finalizeServiceOrder;

  public FinalizeServiceOrderController(FinalizeServiceOrder finalizeServiceOrder) {
    this.finalizeServiceOrder = finalizeServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      var serviceOrder =
          finalizeServiceOrder.finalize(new FinalizeServiceOrder.Params(serviceOrderId));

      return HttpHelper.ok(serviceOrder);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotFoundError | ServicePriceNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (InvalidServiceOrderStatusError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
