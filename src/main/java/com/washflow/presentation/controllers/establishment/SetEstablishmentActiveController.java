package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/** {@code PATCH /api/admin/establishments/{id}/active}: suspends or reactivates a lava-jato. */
public class SetEstablishmentActiveController implements Controller {

  private final ManagePlatform platform;

  public SetEstablishmentActiveController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      Boolean active = BodyValues.bool(body, "active");
      if (active == null) {
        throw new IllegalArgumentException("Informe active true ou false");
      }
      UUID id = UUID.fromString(request.pathParams().get("id"));
      return HttpHelper.ok(platform.setActive(id, active));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
