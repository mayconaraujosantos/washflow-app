package com.washflow.presentation.controllers.resumeserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.washflow.domain.errors.ServiceOrderNotWashingError;
import com.washflow.domain.usecases.ResumeServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeServiceOrderControllerTest {

  @Mock private ResumeServiceOrder resumeServiceOrder;

  private ResumeServiceOrderController sut;
  private UUID serviceOrderId;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new ResumeServiceOrderController(resumeServiceOrder);
    serviceOrderId = UUID.randomUUID();
    washerId = UUID.randomUUID();
  }

  @Test
  void resumesServiceOrderAndReturnsOk() throws Exception {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(resumeServiceOrder).resume(any(ResumeServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsConflictWhenServiceOrderNotWashing() throws Exception {
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("createdBy", washerId);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    doThrow(new ServiceOrderNotWashingError())
        .when(resumeServiceOrder)
        .resume(any(ResumeServiceOrder.Params.class));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
