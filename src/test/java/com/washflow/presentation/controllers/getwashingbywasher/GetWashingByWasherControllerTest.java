package com.washflow.presentation.controllers.getwashingbywasher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.usecases.GetWashingByWasher;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetWashingByWasherControllerTest {

  @Mock private GetWashingByWasher getWashingByWasher;

  private GetWashingByWasherController sut;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new GetWashingByWasherController(getWashingByWasher);
    washerId = UUID.randomUUID();
  }

  @Test
  void getsWashingByWasherAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    List<ServiceOrder> orders =
        List.of(
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                washerId,
                ServiceOrderStatus.WASHING,
                Instant.now(),
                Instant.now(),
                Instant.now()));
    when(getWashingByWasher.get(washerId)).thenReturn(orders);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(getWashingByWasher).get(washerId);
  }

  @Test
  void returnsEmptyListWhenNoOrders() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(getWashingByWasher.get(washerId)).thenReturn(List.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
