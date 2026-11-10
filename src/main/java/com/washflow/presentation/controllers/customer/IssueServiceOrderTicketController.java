package com.washflow.presentation.controllers.customer;

import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * {@code POST /api/service-orders/{id}/ticket}: the manager issues the order's public ticket token.
 */
public class IssueServiceOrderTicketController implements Controller {

  private final ServiceOrderTicket ticket;

  public IssueServiceOrderTicketController(ServiceOrderTicket ticket) {
    this.ticket = ticket;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(
          Map.of("token", ticket.issue(UUID.fromString(request.pathParams().get("id")))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
