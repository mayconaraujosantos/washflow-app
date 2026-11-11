package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.errors.establishment.EstablishmentSlugTakenError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/** {@code POST /api/admin/establishments}: creates a lava-jato with its first manager. */
public class CreateEstablishmentController implements Controller {

  private final ManagePlatform platform;

  public CreateEstablishmentController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          platform.create(
              new ManagePlatform.NewEstablishment(
                  BodyValues.text(body, "name"),
                  BodyValues.text(body, "slug"),
                  BodyValues.integer(body, "openingHour"),
                  BodyValues.integer(body, "closingHour"),
                  Boolean.TRUE.equals(BodyValues.bool(body, "openOnSunday")),
                  BodyValues.integer(body, "maxVehiclesPerHour"),
                  BodyValues.text(body, "managerName"),
                  BodyValues.text(body, "managerEmail"),
                  BodyValues.text(body, "managerPhone"),
                  BodyValues.text(body, "managerPassword"))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (EstablishmentSlugTakenError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
