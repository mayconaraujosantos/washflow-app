package com.washflow.presentation.controllers.whatsapp;

import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/**
 * {@code POST /api/whatsapp/connection} (manager): the QR code to link the lava-jato's number. The
 * code expires in about a minute; calling again returns a fresh one.
 */
public class ConnectWhatsappController implements Controller {

  private final WhatsappConnection connection;

  public ConnectWhatsappController(WhatsappConnection connection) {
    this.connection = connection;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(connection.connect());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
