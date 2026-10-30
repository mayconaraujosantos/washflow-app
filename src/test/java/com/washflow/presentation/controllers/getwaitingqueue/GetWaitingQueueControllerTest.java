package com.washflow.presentation.controllers.getwaitingqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.usecases.GetWaitingQueue;
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
class GetWaitingQueueControllerTest {

  @Mock private GetWaitingQueue getWaitingQueue;

  private GetWaitingQueueController sut;

  @BeforeEach
  void setUp() {
    sut = new GetWaitingQueueController(getWaitingQueue);
  }

  @Test
  void getsWaitingQueueAndReturnsOk() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());

    List<ServiceOrder> queue =
        List.of(
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                null,
                ServiceOrderStatus.WAITING_IN_YARD,
                Instant.now(),
                Instant.now(),
                Instant.now()));
    when(getWaitingQueue.get()).thenReturn(queue);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(getWaitingQueue).get();
  }

  @Test
  void returnsEmptyListWhenNoQueue() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());

    when(getWaitingQueue.get()).thenReturn(List.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
