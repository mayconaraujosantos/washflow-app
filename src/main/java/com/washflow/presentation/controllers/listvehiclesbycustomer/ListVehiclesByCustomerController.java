package com.washflow.presentation.controllers.listvehiclesbycustomer;

import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListVehiclesByCustomer;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class ListVehiclesByCustomerController implements Controller {

  private final ListVehiclesByCustomer listVehiclesByCustomer;

  public ListVehiclesByCustomerController(ListVehiclesByCustomer listVehiclesByCustomer) {
    this.listVehiclesByCustomer = listVehiclesByCustomer;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID customerId = UUID.fromString(request.pathParams().get("id"));

      var vehicles = listVehiclesByCustomer.list(new ListVehiclesByCustomer.Params(customerId));

      return HttpHelper.ok(vehicles);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
