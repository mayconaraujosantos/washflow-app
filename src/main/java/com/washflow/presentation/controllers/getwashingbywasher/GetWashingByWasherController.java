package com.washflow.presentation.controllers.getwashingbywasher;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.GetWashingByWasher;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.UUID;

/**
 * Parses the washer ID from the authenticated user and delegates to {@code GetWashingByWasher}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class GetWashingByWasherController implements Controller {

  private final GetWashingByWasher getWashingByWasher;

  public GetWashingByWasherController(GetWashingByWasher getWashingByWasher) {
    this.getWashingByWasher = getWashingByWasher;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    UUID washerId = (UUID) request.body().get("createdBy");
    List<ServiceOrder> orders = getWashingByWasher.get(washerId);
    return new HttpResponse(200, orders);
  }
}
