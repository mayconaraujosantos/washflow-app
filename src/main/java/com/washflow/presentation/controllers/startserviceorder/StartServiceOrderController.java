package com.washflow.presentation.controllers.startserviceorder;

import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.StartServiceOrder;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.util.UUID;

public class StartServiceOrderController implements Controller {

  private final StartServiceOrder startServiceOrder;
  private final Validation validation;

  public StartServiceOrderController(StartServiceOrder startServiceOrder, Validation validation) {
    this.startServiceOrder = startServiceOrder;
    this.validation = validation;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      UUID washerId = UUID.fromString((String) request.body().get("washerId"));

      var serviceOrder =
          startServiceOrder.start(new StartServiceOrder.Params(serviceOrderId, washerId));

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
