package com.washflow.presentation.controllers.whatsapp;

import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code DELETE /api/whatsapp/connection} (manager): unlinks the lava-jato's number. */
public class DisconnectWhatsappController implements Controller {

  private final WhatsappConnection connection;

  public DisconnectWhatsappController(WhatsappConnection connection) {
    this.connection = connection;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      connection.disconnect();
      return HttpHelper.ok(connection.status());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
