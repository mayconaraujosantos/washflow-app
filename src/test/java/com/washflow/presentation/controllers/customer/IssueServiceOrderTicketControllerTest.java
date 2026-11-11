package com.washflow.presentation.controllers.customer;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssueServiceOrderTicketControllerTest {

  @Test
  void issuesTheTicketOfTheOrderInThePath() throws Exception {
    ServiceOrderTicket ticket = mock(ServiceOrderTicket.class);
    UUID id = UUID.randomUUID();
    when(ticket.issue(id)).thenReturn("abc");

    HttpResponse response =
        new IssueServiceOrderTicketController(ticket).handle(request(Map.of("id", id.toString())));

    assertEquals(200, response.statusCode());
    assertEquals(Map.of("token", "abc"), response.body());
  }

  @Test
  void rejectsAnInvalidIdAndAnUnknownOrder() throws Exception {
    ServiceOrderTicket ticket = mock(ServiceOrderTicket.class);
    when(ticket.issue(any())).thenThrow(new ServiceOrderNotFoundError(UUID.randomUUID()));
    IssueServiceOrderTicketController sut = new IssueServiceOrderTicketController(ticket);

    assertEquals(400, sut.handle(request(Map.of("id", "x"))).statusCode());
    assertEquals(404, sut.handle(request(Map.of("id", UUID.randomUUID().toString()))).statusCode());
  }
}
