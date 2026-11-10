package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/** {@code PUT /api/establishment} (manager): updates name, hours, Sundays and capacity. */
public class UpdateEstablishmentController implements Controller {

  private final ManageEstablishment manageEstablishment;

  public UpdateEstablishmentController(ManageEstablishment manageEstablishment) {
    this.manageEstablishment = manageEstablishment;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          manageEstablishment.update(
              new ManageEstablishment.Params(
                  BodyValues.text(body, "name"),
                  BodyValues.integer(body, "openingHour"),
                  BodyValues.integer(body, "closingHour"),
                  Boolean.TRUE.equals(BodyValues.bool(body, "openOnSunday")),
                  BodyValues.integer(body, "maxVehiclesPerHour"))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
