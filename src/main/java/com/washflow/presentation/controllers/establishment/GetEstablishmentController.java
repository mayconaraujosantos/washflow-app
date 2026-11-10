package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/**
 * {@code GET /api/establishment} (public, inside the lava-jato): name, hours, Sundays and capacity.
 */
public class GetEstablishmentController implements Controller {

  private final ManageEstablishment manageEstablishment;

  public GetEstablishmentController(ManageEstablishment manageEstablishment) {
    this.manageEstablishment = manageEstablishment;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(manageEstablishment.get());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
