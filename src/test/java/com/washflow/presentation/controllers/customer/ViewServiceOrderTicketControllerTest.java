package com.washflow.presentation.controllers.customer;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.serviceorder.TicketView;
import com.washflow.domain.errors.common.TicketNotFoundError;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ViewServiceOrderTicketControllerTest {

  @Test
  void showsTheTicketOfTheTokenInThePath() throws Exception {
    ServiceOrderTicket ticket = mock(ServiceOrderTicket.class);
    TicketView view = mock(TicketView.class);
    when(ticket.view("abc")).thenReturn(view);

    HttpResponse response =
        new ViewServiceOrderTicketController(ticket).handle(request(Map.of("token", "abc")));

    assertEquals(200, response.statusCode());
    assertEquals(view, response.body());
  }

  @Test
  void anUnknownTokenIsNotFound() throws Exception {
    ServiceOrderTicket ticket = mock(ServiceOrderTicket.class);
    when(ticket.view(any())).thenThrow(new TicketNotFoundError());

    assertEquals(
        404,
        new ViewServiceOrderTicketController(ticket)
            .handle(request(Map.of("token", "x")))
            .statusCode());
  }
}
