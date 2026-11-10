package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.Map;

/** {@code GET} (public) and {@code PUT} (manager) {@code /api/establishment}. */
public final class EstablishmentController {

  private EstablishmentController() {}

  public static Controller get(ManageEstablishment manageEstablishment) {
    return request -> {
      try {
        return HttpHelper.ok(manageEstablishment.get());
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller update(ManageEstablishment manageEstablishment) {
    return (HttpRequest request) -> {
      try {
        Map<String, Object> body = request.body() == null ? Map.of() : request.body();
        return HttpHelper.ok(
            manageEstablishment.update(
                new ManageEstablishment.Params(
                    BodyValues.text(body, "name"),
                    BodyValues.integer(body, "openingHour"),
                    BodyValues.integer(body, "closingHour"),
                    Boolean.TRUE.equals(body.get("openOnSunday")),
                    BodyValues.integer(body, "maxVehiclesPerHour"))));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }
}
