package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.errors.establishment.EstablishmentNotFoundError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * {@code PATCH /api/admin/establishments/{id}/manager-password} with {@code {"password": "..."}}:
 * the platform administrator sets a new password for the lava-jato's manager.
 */
public class ResetManagerPasswordController implements Controller {

  private final ManagePlatform platform;

  public ResetManagerPasswordController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      UUID id = UUID.fromString(request.pathParams().get("id"));
      return HttpHelper.ok(platform.resetManagerPassword(id, BodyValues.text(body, "password")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (EstablishmentNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
