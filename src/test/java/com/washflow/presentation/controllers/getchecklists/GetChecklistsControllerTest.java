package com.washflow.presentation.controllers.getchecklists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import com.washflow.domain.usecases.ListServiceOrderChecklists;
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
class GetChecklistsControllerTest {

  @Mock private ListServiceOrderChecklists listServiceOrderChecklists;

  private GetChecklistsController sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new GetChecklistsController(listServiceOrderChecklists);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void returnsChecklistsWhenExist() {
    List<ChecklistItem> items = List.of(new ChecklistItem("Faróis", true, null));
    ServiceOrderChecklist checklist =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            serviceOrderId,
            ChecklistType.ENTRY,
            items,
            null,
            Instant.now(),
            UUID.randomUUID());
    when(listServiceOrderChecklists.list(serviceOrderId)).thenReturn(List.of(checklist));

    HttpRequest request =
        new HttpRequest(Map.of(), Map.of("id", serviceOrderId.toString()), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    assertEquals(1, ((List<?>) response.body()).size());
    verify(listServiceOrderChecklists).list(serviceOrderId);
  }

  @Test
  void returnsEmptyListWhenNoChecklists() {
    when(listServiceOrderChecklists.list(serviceOrderId)).thenReturn(List.of());

    HttpRequest request =
        new HttpRequest(Map.of(), Map.of("id", serviceOrderId.toString()), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    assertEquals(0, ((List<?>) response.body()).size());
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
