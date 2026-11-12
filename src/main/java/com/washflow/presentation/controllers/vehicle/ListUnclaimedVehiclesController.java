package com.washflow.presentation.controllers.vehicle;

import com.washflow.domain.usecases.vehicle.ListUnclaimedVehicles;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class ListUnclaimedVehiclesController implements Controller {

  private final ListUnclaimedVehicles listUnclaimedVehicles;

  public ListUnclaimedVehiclesController(ListUnclaimedVehicles listUnclaimedVehicles) {
    this.listUnclaimedVehicles = listUnclaimedVehicles;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID customerId = UUID.fromString(request.pathParams().get("id"));
      return HttpHelper.ok(listUnclaimedVehicles.list(customerId));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
