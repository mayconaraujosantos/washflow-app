package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code GET /api/admin/establishments}: every lava-jato, for the platform admin. */
public class ListEstablishmentsController implements Controller {

  private final ManagePlatform platform;

  public ListEstablishmentsController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(platform.list());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
