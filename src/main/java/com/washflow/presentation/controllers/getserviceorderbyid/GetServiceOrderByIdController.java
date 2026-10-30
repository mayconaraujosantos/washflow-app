package com.washflow.presentation.controllers.getserviceorderbyid;

import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.GetServiceOrderById;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class GetServiceOrderByIdController implements Controller {

  private final GetServiceOrderById getServiceOrderById;

  public GetServiceOrderByIdController(GetServiceOrderById getServiceOrderById) {
    this.getServiceOrderById = getServiceOrderById;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      var serviceOrder = getServiceOrderById.get(new GetServiceOrderById.Params(serviceOrderId));

      return HttpHelper.ok(serviceOrder);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
