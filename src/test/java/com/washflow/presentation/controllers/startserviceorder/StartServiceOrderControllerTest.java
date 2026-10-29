package com.washflow.presentation.controllers.startserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.StartServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartServiceOrderControllerTest {

  @Mock private StartServiceOrder startServiceOrder;
  @Mock private Validation validation;

  private StartServiceOrderController sut;
  private UUID serviceOrderId;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new StartServiceOrderController(startServiceOrder, validation);
    serviceOrderId = UUID.randomUUID();
    washerId = UUID.randomUUID();
  }

  @Test
  void startsServiceOrderAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("washerId", washerId.toString());
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(null);
    ServiceOrder saved =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            washerId,
            ServiceOrderStatus.WASHING,
            Instant.now(),
            Instant.now(),
            Instant.now());
    when(startServiceOrder.start(any(StartServiceOrder.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(startServiceOrder).start(any(StartServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenValidationFails() {
    Map<String, Object> body = new HashMap<>();
    body.put("washerId", washerId.toString());
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(new IllegalArgumentException("missing field"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    Map<String, Object> body = new HashMap<>();
    body.put("washerId", washerId.toString());
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    when(validation.validate(body)).thenReturn(null);

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenServiceOrderNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("washerId", washerId.toString());
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(startServiceOrder.start(any(StartServiceOrder.Params.class)))
        .thenThrow(new ServiceOrderNotFoundError(serviceOrderId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenInvalidStatus() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("washerId", washerId.toString());
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(startServiceOrder.start(any(StartServiceOrder.Params.class)))
        .thenThrow(
            new InvalidServiceOrderStatusError(
                ServiceOrderStatus.SCHEDULED, ServiceOrderStatus.WASHING));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
