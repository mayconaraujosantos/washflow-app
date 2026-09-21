package com.washflow.presentation.controllers.listserviceordersbycustomer;

import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListServiceOrdersByCustomer;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class ListServiceOrdersByCustomerController implements Controller {

  private final ListServiceOrdersByCustomer listServiceOrdersByCustomer;

  public ListServiceOrdersByCustomerController(
      ListServiceOrdersByCustomer listServiceOrdersByCustomer) {
    this.listServiceOrdersByCustomer = listServiceOrdersByCustomer;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID customerId = UUID.fromString(request.pathParams().get("id"));

      var serviceOrders =
          listServiceOrdersByCustomer.list(new ListServiceOrdersByCustomer.Params(customerId));

      return HttpHelper.ok(serviceOrders);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
