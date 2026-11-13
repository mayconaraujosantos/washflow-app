package com.washflow.presentation.controllers.whatsapp;

import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code GET /api/whatsapp/connection} (manager): is the lava-jato's number linked? */
public class GetWhatsappConnectionController implements Controller {

  private final WhatsappConnection connection;

  public GetWhatsappConnectionController(WhatsappConnection connection) {
    this.connection = connection;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(connection.status());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
