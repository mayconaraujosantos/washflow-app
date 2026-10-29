package com.washflow.presentation.controllers.pauseserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.washflow.domain.errors.ServiceOrderNotPausedError;
import com.washflow.domain.usecases.PauseServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PauseServiceOrderControllerTest {

  @Mock private PauseServiceOrder pauseServiceOrder;

  private PauseServiceOrderController sut;
  private UUID serviceOrderId;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new PauseServiceOrderController(pauseServiceOrder);
    serviceOrderId = UUID.randomUUID();
    washerId = UUID.randomUUID();
  }

  @Test
  void pausesServiceOrderAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("reason", "LUNCH");
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(pauseServiceOrder).pause(any(PauseServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    Map<String, Object> body = new HashMap<>();
    body.put("reason", "LUNCH");
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsConflictWhenServiceOrderNotPaused() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("reason", "LUNCH");
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    doThrow(new ServiceOrderNotPausedError())
        .when(pauseServiceOrder)
        .pause(any(PauseServiceOrder.Params.class));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
