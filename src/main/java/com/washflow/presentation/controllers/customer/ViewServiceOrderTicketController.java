package com.washflow.presentation.controllers.customer;

import com.washflow.domain.errors.common.TicketNotFoundError;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/**
 * {@code GET /api/tickets/{token}} (public): vehicle, service and progress, without customer data.
 */
public class ViewServiceOrderTicketController implements Controller {

  private final ServiceOrderTicket ticket;

  public ViewServiceOrderTicketController(ServiceOrderTicket ticket) {
    this.ticket = ticket;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(ticket.view(request.pathParams().get("token")));
    } catch (TicketNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
