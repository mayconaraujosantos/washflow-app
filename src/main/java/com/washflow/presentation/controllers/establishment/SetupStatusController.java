package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code GET /api/setup}: public, tells the app whether to show the setup wizard. */
public class SetupStatusController implements Controller {

  private final SetupEstablishment setupEstablishment;

  public SetupStatusController(SetupEstablishment setupEstablishment) {
    this.setupEstablishment = setupEstablishment;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(setupEstablishment.status());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
