package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/auth/admins}: returns the platform admin, turned into a session by the adapter.
 */
public class AdminSignInController implements Controller {

  private final ManagePlatform platform;

  public AdminSignInController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          platform.signIn(BodyValues.text(body, "email"), BodyValues.text(body, "password")));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
