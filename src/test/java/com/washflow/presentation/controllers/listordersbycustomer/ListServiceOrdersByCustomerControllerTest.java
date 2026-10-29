package com.washflow.presentation.controllers.listserviceordersbycustomer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.usecases.ListServiceOrdersByCustomer;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListServiceOrdersByCustomerControllerTest {

  @Mock private ListServiceOrdersByCustomer listServiceOrdersByCustomer;

  private ListServiceOrdersByCustomerController sut;
  private UUID customerId;

  @BeforeEach
  void setUp() {
    sut = new ListServiceOrdersByCustomerController(listServiceOrdersByCustomer);
    customerId = UUID.randomUUID();
  }

  @Test
  void listsServiceOrdersAndReturnsOk() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", customerId.toString()), Map.of());

    List<ServiceOrder> orders =
        List.of(
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                null,
                ServiceOrderStatus.SCHEDULED,
                Instant.now(),
                Instant.now(),
                Instant.now()));
    when(listServiceOrdersByCustomer.list(any(ListServiceOrdersByCustomer.Params.class)))
        .thenReturn(orders);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listServiceOrdersByCustomer).list(any(ListServiceOrdersByCustomer.Params.class));
  }

  @Test
  void returnsBadRequestWhenCustomerIdIsInvalid() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsEmptyListWhenNoOrders() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", customerId.toString()), Map.of());

    when(listServiceOrdersByCustomer.list(any(ListServiceOrdersByCustomer.Params.class)))
        .thenReturn(List.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
